import {IOpticTaskRunnerConfig} from '@useoptic/cli-config';
import {ICaptureSaver} from '@useoptic/cli-server';
import {HttpToolkitCapturingProxy} from '@useoptic/proxy';
import {IApiInteraction} from '@useoptic/proxy';
import {CommandSession} from './command-session';
import {userDebugLogger} from './logger';

class CommandAndProxySessionManager {
  constructor(private config: IOpticTaskRunnerConfig) {

  }

  async run(persistenceManager: ICaptureSaver) {
    const commandSession = new CommandSession();
    const inboundProxy = new HttpToolkitCapturingProxy();
    const servicePort = this.config.serviceConfig.port;
    const serviceHost = this.config.serviceConfig.host;
    const opticServiceConfig = {
      OPTIC_API_PORT: servicePort.toString(),
      OPTIC_API_HOST: serviceHost.toString(),
    };

    await persistenceManager.init(this.config.captureId);

    inboundProxy.events.on('sample', (sample: IApiInteraction) => {
      userDebugLogger(`got sample ${sample.request.method} ${sample.request.url}`);
      persistenceManager.save(sample);
    });

    const target = new URL('https://example.org')
    target.host = serviceHost
    target.port = servicePort.toString()
    target.protocol = this.config.serviceConfig.protocol

    await inboundProxy.start({
      flags: {
        chrome: false
      },
      proxyPort: this.config.proxyConfig.port,
      proxyTarget: target.host.toString()
    });

    userDebugLogger(`started inbound proxy on port ${servicePort}`);
    const promises = [];
    if (this.config.command) {
      await commandSession.start({
        command: this.config.command,
        // @ts-ignore
        environmentVariables: {
          ...process.env,
          ...opticServiceConfig
        }
      });
      const commandStoppedPromise = new Promise(resolve => {
        commandSession.events.on('stopped', () => resolve());
      });
      promises.push(commandStoppedPromise);
    }

    const processInterruptedPromise = new Promise((resolve) => {
      process.on('SIGINT', () => {
        resolve();
      });
    });
    promises.push(processInterruptedPromise);

    await Promise.race(promises);

    commandSession.stop();
    await Promise.all(promises);
    await inboundProxy.stop();
  }
}

export {
  CommandAndProxySessionManager
};
