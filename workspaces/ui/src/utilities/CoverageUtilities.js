import {StableHashableWrapper} from '@useoptic/domain';
import crypto from 'crypto';

const log = [];
global.opticHashLog = log;

function hasher(input) {
  const sha1 = crypto.createHash('sha1');
  sha1.update(input);
  const hex = sha1.digest('hex');
  log.push({
    input,
    output: hex
  });
  return hex;
}

debugger
export const StableHasher = StableHashableWrapper(hasher);