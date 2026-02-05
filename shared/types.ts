export type NetInterfaceInfo = {
  name: string;
  ipv4: string;
};

export type DeviceInfo = {
  udid: string;
  state: string;
  model: string;
  manufacturer: string;
  androidRelease: string;
  androidSdk: string;
  abi: string;
  interfaces: NetInterfaceInfo[];
};

export type RoleInfo = {
  id: number;
  code: string;
  name: string;
};

export type UserInfo = {
  id: number;
  username: string;
  status: 'ACTIVE' | 'DISABLED';
  createdAt: string;
  superAdmin: boolean;
  roles: RoleInfo[];
  permissions: string[];
};

export type PermissionInfo = {
  id: number;
  code: string;
  name: string;
  description?: string;
};

export type UserRef = {
  id: number;
  username: string;
  status: 'ACTIVE' | 'DISABLED';
  superAdmin: boolean;
};

export type DeviceGroupInfo = {
  id: number;
  name: string;
  remark?: string;
  deviceCount: number;
};

export type GroupRef = {
  id: number;
  name: string;
};

export type DeviceDto = {
  deviceId: string;
  state: string;
  alias?: string;
  remark?: string;
  manufacturer?: string;
  model?: string;
  androidRelease?: string;
  androidSdk?: string;
  groups: GroupRef[];
};

export type DeviceGroupDetail = {
  id: number;
  name: string;
  remark?: string;
  devices: DeviceDto[];
};

export type UserGroupInfo = {
  id: number;
  name: string;
  remark?: string;
  userCount: number;
  deviceGroupCount: number;
};

export type UserGroupDetail = {
  id: number;
  name: string;
  remark?: string;
  users: UserRef[];
  deviceGroups: DeviceGroupInfo[];
};
