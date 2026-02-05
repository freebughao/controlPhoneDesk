import React, { useEffect, useRef, useState } from 'react';
import { ScrcpyStream } from '../stream/ScrcpyStream';
import { WebCodecsPlayer } from '../legacy/app/player/WebCodecsPlayer';
import { MsePlayer } from '../legacy/app/player/MsePlayer';
import { FeaturedInteractionHandler } from '../legacy/app/interactionHandler/FeaturedInteractionHandler';
import { CommandControlMessage } from '../legacy/app/controlMessage/CommandControlMessage';

type StreamDevice = {
  deviceId: string;
  manufacturer?: string;
  model?: string;
};

type StreamPaneProps = {
  device: StreamDevice | null;
  apiBase: string;
  tokenProvider?: () => string;
};

const selectPlayer = () => {
  if (WebCodecsPlayer.isSupported()) {
    return WebCodecsPlayer;
  }
  if (MsePlayer.isSupported()) {
    return MsePlayer;
  }
  return null;
};

export function StreamPane({ device, apiBase, tokenProvider }: StreamPaneProps): JSX.Element {
  const containerRef = useRef<HTMLDivElement>(null);
  const [status, setStatus] = useState('idle');

  useEffect(() => {
    const container = containerRef.current;
    if (!device || !container) {
      if (container) {
        container.replaceChildren();
      }
      setStatus('idle');
      return;
    }

    const PlayerClass = selectPlayer();
    if (!PlayerClass) {
      setStatus('当前浏览器不支持 WebCodecs/MSE');
      return;
    }

    let handler: FeaturedInteractionHandler | undefined;
    let stream: ScrcpyStream | undefined;
    container.replaceChildren();

    // 创建解码器并挂载到容器
    const player = new PlayerClass(device.deviceId);
    player.setParent(container);
    player.play();

    // 建立 WebSocket 流并将视频帧送给解码器
    const splitNalUnits = PlayerClass === WebCodecsPlayer;
    stream = new ScrcpyStream(
      apiBase,
      device.deviceId,
      {
        onVideo: (data) => {
          player.pushFrame(data);
        },
        onOpen: () => {
          // 发送初始视频配置到设备端
          const settings = player.getPreferredVideoSetting();
          player.setVideoSettings(settings, false, false);
          const cmd = CommandControlMessage.createSetVideoSettingsCommand(settings);
          stream?.send(cmd.toBuffer());
          setStatus(`connected (${PlayerClass.playerFullName})`);
        },
        onClose: () => setStatus('disconnected'),
        onError: () => setStatus('error')
      },
      { splitNalUnits },
      tokenProvider
    );

    stream.connect();
    // 绑定鼠标/触摸交互并透传到设备端
    handler = new FeaturedInteractionHandler(player, {
      sendMessage: (message) => {
        stream?.send(message.toBuffer());
      }
    });

    return () => {
      handler?.release();
      stream?.close();
      player.stop();
      container.replaceChildren();
    };
  }, [device?.deviceId, apiBase, tokenProvider]);

  if (!device) {
    return <div className="stream-placeholder">选择一个设备开始投屏</div>;
  }

  return (
    <div>
      <div className="stream-header">
        <strong>{device.manufacturer ?? 'Android'} {device.model ?? device.deviceId}</strong>
        <span className="status">状态: {status}</span>
      </div>
      <div className="stream-container" ref={containerRef}></div>
    </div>
  );
}
