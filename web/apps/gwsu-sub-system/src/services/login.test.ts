import {buildDingTalkCompleteParams, TerminalType} from './login';

declare const describe: (name: string, fn: () => void) => void;
declare const it: (name: string, fn: () => void) => void;
declare const expect: (actual: unknown) => {toEqual(expected: unknown): void};

describe('钉钉首次登录续办请求', () => {
    it('构造绑定已有账号请求', () => {
        expect(buildDingTalkCompleteParams('binding', 'voucher-1', {
            bindingToken: 'token-1',
        })).toEqual({
            type: 'dingtalk',
            terminal: TerminalType.WEB,
            extraParam: {
                createMethod: ['binding'],
                temporaryVoucher: ['voucher-1'],
                bindingToken: ['token-1'],
            },
        });
    });

    it('构造创建新账号请求', () => {
        expect(buildDingTalkCompleteParams('create', 'voucher-2', {
            username: 'new-user',
            password: 'md5-password',
        })).toEqual({
            type: 'dingtalk',
            terminal: TerminalType.WEB,
            extraParam: {
                createMethod: ['create'],
                temporaryVoucher: ['voucher-2'],
                username: ['new-user'],
                password: ['md5-password'],
            },
        });
    });
});
