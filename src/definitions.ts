declare module "@capacitor/core" {
  interface PluginRegistry {
    CapacitorPluginPolar: CapacitorPluginPolarPlugin;
  }
}

/**
 * The Capacitor polar plugin allows you to interact with polar device
 *
 * Use to:
 * - connect to polar device with device_id
 * - receive polar data from device
 *
 */
export interface CapacitorPluginPolarPlugin {

  /**
   * Connect to polar device
   */
  connect(options: { deviceId: string }): Promise<{value: string}>;

  /**
   * Disconnect to polar device
   */
  disconnect(options: { deviceId: string }): Promise<{value: string}>;

}
