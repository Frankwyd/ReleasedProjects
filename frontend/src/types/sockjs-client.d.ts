declare module 'sockjs-client' {
  class SockJS {
    constructor(url: string, _reserved?: any, options?: any);
    close(): void;
    send(data: string): void;
    onopen: () => void;
    onclose: () => void;
    onmessage: (e: { data: string }) => void;
  }
  export default SockJS;
} 