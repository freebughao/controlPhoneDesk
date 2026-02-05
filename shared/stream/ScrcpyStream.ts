import Util from '../legacy/app/Util';
import DeviceMessage from '../legacy/app/googDevice/DeviceMessage';

// scrcpy 初始握手帧标识（用于过滤非视频帧）
const MAGIC_BYTES_INITIAL = Util.stringToUtf8ByteArray('scrcpy_initial');

type StreamHandlers = {
  onVideo: (data: Uint8Array) => void;
  onOpen?: () => void;
  onClose?: () => void;
  onError?: (err: Event) => void;
};

type StreamOptions = {
  splitNalUnits?: boolean;
};

type TokenProvider = () => string;

export class ScrcpyStream {
  private ws?: WebSocket;
  private loggedFirstFrame = false;
  private debugFrameCount = 0;
  private debugStats = {
    sps: 0,
    pps: 0,
    idr: 0,
    other: 0
  };
  private tokenProvider: TokenProvider;

  constructor(
    private baseUrl: string,
    private udid: string,
    private handlers: StreamHandlers,
    private options: StreamOptions = {},
    tokenProvider?: TokenProvider
  ) {
    this.tokenProvider = tokenProvider ?? (() => '');
  }

  private hasStartCodeAt(data: Uint8Array, index: number): boolean {
    if (index + 3 >= data.length) {
      return false;
    }
    if (data[index] === 0x00 && data[index + 1] === 0x00) {
      if (data[index + 2] === 0x01) {
        return true;
      }
      if (data[index + 2] === 0x00 && data[index + 3] === 0x01) {
        return true;
      }
    }
    return false;
  }

  // 尝试从数据中定位 H.264 NALU 起始码，处理可能存在的帧元数据前缀
  private normalizeFrame(data: Uint8Array): Uint8Array {
    // 常见情况：开头即 start code
    if (this.hasStartCodeAt(data, 0)) {
      return data;
    }
    // 兼容 sendFrameMeta 情况：元数据长度通常为 12 字节
    if (data.length > 12 && this.hasStartCodeAt(data, 12)) {
      return data.subarray(12);
    }
    // 兜底：在前 64 字节内搜索 start code
    const limit = Math.min(data.length - 3, 64);
    for (let i = 0; i < limit; i += 1) {
      if (this.hasStartCodeAt(data, i)) {
        return data.subarray(i);
      }
    }
    return data;
  }

  // 将一条消息拆成多个 NALU（按 start code 分割）
  private splitNalUnits(data: Uint8Array): Uint8Array[] {
    const starts: number[] = [];
    for (let i = 0; i < data.length - 3; i += 1) {
      if (this.hasStartCodeAt(data, i)) {
        starts.push(i);
        if (data[i + 2] === 0x01) {
          i += 2;
        } else if (data[i + 3] === 0x01) {
          i += 3;
        }
      }
    }
    if (starts.length <= 1) {
      return [data];
    }
    const units: Uint8Array[] = [];
    for (let i = 0; i < starts.length; i += 1) {
      const start = starts[i];
      const end = i + 1 < starts.length ? starts[i + 1] : data.length;
      units.push(data.subarray(start, end));
    }
    return units;
  }

  connect(): void {
    // 与后端 /ws/scrcpy 建立 WebSocket 连接
    const wsUrl = this.buildWsUrl();
    const ws = new WebSocket(wsUrl);
    ws.binaryType = 'arraybuffer';
    ws.onopen = () => {
      this.handlers.onOpen?.();
    };
    ws.onerror = (event) => {
      this.handlers.onError?.(event);
    };
    ws.onclose = () => {
      this.handlers.onClose?.();
    };
    ws.onmessage = (event) => {
      if (!(event.data instanceof ArrayBuffer)) {
        return;
      }
      const data = new Uint8Array(event.data);
      // 过滤 scrcpy 控制帧（initial / message），其余视为视频帧
      if (data.byteLength >= MAGIC_BYTES_INITIAL.length) {
        let matchInitial = true;
        for (let i = 0; i < MAGIC_BYTES_INITIAL.length; i += 1) {
          if (data[i] !== MAGIC_BYTES_INITIAL[i]) {
            matchInitial = false;
            break;
          }
        }
        if (matchInitial) {
          return;
        }
        let matchMessage = true;
        if (data.byteLength >= DeviceMessage.MAGIC_BYTES_MESSAGE.length) {
          for (let i = 0; i < DeviceMessage.MAGIC_BYTES_MESSAGE.length; i += 1) {
            if (data[i] !== DeviceMessage.MAGIC_BYTES_MESSAGE[i]) {
              matchMessage = false;
              break;
            }
          }
          if (matchMessage) {
            return;
          }
        }
      }
      const normalized = this.normalizeFrame(data);
      if (!this.loggedFirstFrame) {
        this.loggedFirstFrame = true;
        const rawHex = Array.from(data.subarray(0, 16))
          .map((b) => b.toString(16).padStart(2, '0'))
          .join(' ');
        const normHex = Array.from(normalized.subarray(0, 16))
          .map((b) => b.toString(16).padStart(2, '0'))
          .join(' ');
        // 控制台输出原始帧与归一化帧的前 16 字节
        console.log('[scrcpy] first frame raw[0..15]=', rawHex);
        console.log('[scrcpy] first frame normalized[0..15]=', normHex);
      }

      const shouldSplit = this.options.splitNalUnits !== false;
      const units = shouldSplit ? this.splitNalUnits(normalized) : [normalized];
      for (const unit of units) {
        if (this.debugFrameCount < 60 && unit.length > 4) {
          const startCodeLength = unit[2] === 0x01 ? 3 : 4;
          const naluType = unit[startCodeLength] & 31;
          if (naluType === 7) {
            this.debugStats.sps += 1;
          } else if (naluType === 8) {
            this.debugStats.pps += 1;
          } else if (naluType === 5) {
            this.debugStats.idr += 1;
          } else {
            this.debugStats.other += 1;
          }
          this.debugFrameCount += 1;
          if (this.debugFrameCount === 60) {
            console.log('[scrcpy] first 60 NALU stats=', this.debugStats);
          }
        }
        this.handlers.onVideo(unit);
      }
    };
    this.ws = ws;
  }

  send(data: ArrayBuffer | Uint8Array): void {
    // 发送控制指令（触控/按键/设置）
    if (!this.ws || this.ws.readyState !== this.ws.OPEN) {
      return;
    }
    this.ws.send(data);
  }

  close(): void {
    if (this.ws && this.ws.readyState === this.ws.OPEN) {
      this.ws.close();
    }
  }

  private buildWsUrl(): string {
    // 将 http(s) 基址转换成 ws(s) 并拼接 udid
    const base = this.baseUrl.replace(/\/$/, '');
    const wsBase = base.replace(/^http/, 'ws');
    const token = this.tokenProvider();
    const tokenQuery = token ? `&token=${encodeURIComponent(token)}` : '';
    return `${wsBase}/ws/scrcpy?udid=${encodeURIComponent(this.udid)}${tokenQuery}`;
  }
}
