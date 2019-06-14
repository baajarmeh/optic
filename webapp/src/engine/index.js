const scalajs = require('./domain.js')
const commands = require('../fixtures/mattermost-commands.json')

export function newRfcService() {
    return new scalajs.com.seamless.contexts.rfc.RfcService()
}

export const Commands = scalajs.com.seamless.contexts.data_types.Commands
export const Primitives = scalajs.com.seamless.contexts.data_types.Primitives
export const DataTypesHelper = scalajs.com.seamless.contexts.data_types.DataTypesServiceHelper()
export const ContentTypesHelper = scalajs.com.seamless.contexts.requests.ContentTypes()

export const facade = scalajs.com.seamless.contexts.rfc.RfcServiceJSFacade()
export const eventStore = facade.makeEventStore()
export const queries = new scalajs.com.seamless.contexts.rfc.Queries(eventStore)
facade.fromJsonCommands(eventStore, JSON.stringify(commands), 'testId')
const x = queries.paths('testId')
console.log(x.toJsArray)
debugger;
