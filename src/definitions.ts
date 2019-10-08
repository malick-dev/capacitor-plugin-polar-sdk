declare module "@capacitor/core" {
  interface PluginRegistry {
    CapacitorPluginPolar: CapacitorPluginPolarPlugin;
  }
}

export interface CapacitorPluginPolarPlugin {

  /**
   * Defautlt echo plugin method demo - Return the given value string
   */
  echo(options: { value: string }): Promise<{value: string}>;

  /**
   * Custom plugin method for polar connect - Return the given value string
   */
  connect(options: { deviceId: string }): Promise<{value: string}>;

}
