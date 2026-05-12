import React, { useState } from "react";
import { App, Tag } from "antd";
import type { SysAccountBindDTO, SysAccountVO } from "../../types";
import { IDENTITY_TYPE_MAP } from "../../types";
import { bindAccount, unbindAccount } from "@/services/user";
import AccountBindForm from "./AccountBindForm";
import { AuthGate } from "@gwsu/core";

interface AccountBindSectionProps {
  userId: string;
  accounts: SysAccountVO[];
  onRefresh: () => void;
  readOnly?: boolean;
}

const AccountBindSection: React.FC<AccountBindSectionProps> = ({
  userId,
  accounts,
  onRefresh,
  readOnly = false,
}) => {
  const { message } = App.useApp();
  const [expandedType, setExpandedType] = useState<string | null>(null);

  const allTypes = ["password", "phone", "wechat"];
  const accountMap = new Map(accounts.map((a) => [a.identityType, a]));

  const handleBind =
    (_identityType: string) => async (data: SysAccountBindDTO) => {
      try {
        await bindAccount(userId, data);
        message.success("绑定成功");
        setExpandedType(null);
        onRefresh();
      } catch (err) {}
    };

  const handleUnbind = (accountId: string) => {
    unbindAccount(userId, accountId)
      .then((_) => {
        message.success("解绑成功");
        onRefresh();
      })
      .catch((_) => {});
  };

  return (
    <div>
      <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 10 }}>
        账号绑定
      </div>
      <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
        {allTypes.map((type) => {
          const account = accountMap.get(type);
          const typeInfo = IDENTITY_TYPE_MAP[type] || {
            label: type,
            icon: "🔑",
          };

          return (
            <div
              key={type}
              style={{
                display: "flex",
                justifyContent: "space-between",
                alignItems: expandedType === type ? "flex-start" : "center",
                padding: "10px 12px",
                background: "#fafafa",
                borderRadius: 6,
                border: "1px solid #f0f0f0",
                flexDirection: "column",
              }}
            >
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  width: "100%",
                }}
              >
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 8,
                  }}
                >
                  <span style={{ fontSize: 16 }}>{typeInfo.icon}</span>
                  <div>
                    <div style={{ fontSize: 12, fontWeight: 500 }}>
                      {typeInfo.label}
                    </div>
                    <div style={{ fontSize: 10, color: "#999" }}>
                      {account ? account.identifier : "未绑定"}
                    </div>
                  </div>
                </div>
                <div
                  style={{
                    display: "flex",
                    alignItems: "center",
                    gap: 8,
                  }}
                >
                  {account ? (
                    <>
                      <Tag color="success">已绑定</Tag>
                      {!readOnly && (
                        <AuthGate buttonKey="4_edit">
                          <a
                            style={{ color: "#ff4d4f", fontSize: 11 }}
                            data-ai-approval
                            onClick={() => handleUnbind(account.id)}
                          >
                            解绑
                          </a>
                        </AuthGate>
                      )}
                    </>
                  ) : (
                    !readOnly && (
                      <AuthGate buttonKey="4_edit">
                        <a onClick={() => setExpandedType(type)}>绑定</a>
                      </AuthGate>
                    )
                  )}
                </div>
              </div>
              {expandedType === type && (
                <div
                  style={{
                    width: "100%",
                    marginTop: 8,
                    borderTop: "1px dashed #e8e8e8",
                    paddingTop: 8,
                  }}
                >
                  <AccountBindForm
                    identityType={type}
                    onSubmit={handleBind(type)}
                    onCancel={() => setExpandedType(null)}
                  />
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default AccountBindSection;
