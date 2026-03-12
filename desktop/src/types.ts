export type SshAuthType = "PASSWORD" | "PRIVATE_KEY";

export interface RemoteHost {
  id: string;
  name: string;
  address: string;
  port: number;
  username: string;
  authType: SshAuthType;
  privateKeyPath?: string;
}

export interface CreateHostPayload {
  name: string;
  address: string;
  port: number;
  username: string;
  authType: SshAuthType;
  password?: string;
  privateKeyPath?: string;
  privateKeyPassphrase?: string;
}

export interface ContainerSummary {
  id: string;
  names: string;
  image: string;
  state: string;
  status: string;
}

export interface DeleteResponse {
  success: boolean;
  warnings: string[];
}
