declare module "@capacitor/core" {
  interface PluginRegistry {
    CapacitorPluginPolar: CapacitorPluginPolarPlugin;
  }
}

export interface CapacitorPluginPolarPlugin {
  echo(options: { value: string }): Promise<{value: string}>;
}
