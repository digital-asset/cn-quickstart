import { randomBytes } from 'node:crypto'
import type * as damlTypes from '@daml/types'
import type { BackendConfig } from '../config.js'
import type { LedgerApi, DisclosedContract } from './ledger.js'

export interface SubmitContext {
  actAs: string
  userId: string
  commandIdOverride?: string
}

export const generateCommandId = (override?: string): string =>
  override !== undefined && override !== '' ? override : `qs-js-${randomBytes(8).toString('hex')}`

export const createContract = async <T extends object, K, I extends string>(
  ledger: LedgerApi,
  ctx: SubmitContext,
  template: damlTypes.Template<T, K, I>,
  payload: T
): Promise<unknown> => {
  return ledger.submitAndWaitForTransaction({
    commandId: generateCommandId(ctx.commandIdOverride),
    actAs: [ctx.actAs],
    readAs: [ctx.actAs],
    userId: ctx.userId,
    commands: [{
      CreateCommand: {
        templateId: template.templateId,
        createArguments: template.encode(payload)
      }
    }]
  })
}

export const exerciseChoice = async <T extends object, C, R, K>(
  ledger: LedgerApi,
  ctx: SubmitContext,
  template: damlTypes.TemplateOrInterface<T, K>,
  choice: damlTypes.Choice<T, C, R, K>,
  contractId: string,
  args: C,
  disclosed?: DisclosedContract[]
): Promise<unknown> => {
  return ledger.submitAndWaitForTransaction({
    commandId: generateCommandId(ctx.commandIdOverride),
    actAs: [ctx.actAs],
    readAs: [ctx.actAs],
    userId: ctx.userId,
    commands: [{
      ExerciseCommand: {
        templateId: template.templateId,
        contractId,
        choice: choice.choiceName,
        choiceArgument: choice.argumentEncode(args)
      }
    }],
    disclosedContracts: disclosed
  })
}

export const submitContextFromSession = (
  cfg: BackendConfig,
  party: string,
  commandIdQuery: string | undefined
): SubmitContext => ({
  actAs: party,
  userId: cfg.appProviderUserId,
  commandIdOverride: commandIdQuery
})
