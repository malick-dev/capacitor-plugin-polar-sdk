import { WebPlugin } from '@capacitor/core';
import { CapacitorPluginPolarPlugin } from './definitions';

export class CapacitorPluginPolarWeb extends WebPlugin implements CapacitorPluginPolarPlugin {
  constructor() {
    super({
      name: 'CapacitorPluginPolar',
      platforms: ['web']
    });
  }

  async echo(options: { value: string }): Promise<{value: string}> {
    console.log('ECHO', options);
    return options;
  }
}

const CapacitorPluginPolar = new CapacitorPluginPolarWeb();

export { CapacitorPluginPolar };

import { registerWebPlugin } from '@capacitor/core';
registerWebPlugin(CapacitorPluginPolar);
