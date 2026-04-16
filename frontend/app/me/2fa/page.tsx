"use client";

import {useState} from "react";
import {useRouter} from "next/navigation";
import AuthShell from "@/components/AuthShell";
import {buttonCls, inputCls} from "@/components/ui";
import {confirmTwoFactor, disableTwoFactor, setupTwoFactor} from "@/lib/api";

export default function TwoFactorPage() {
    const router = useRouter();
    const [loading, setLoading] = useState(false);
    const [msg, setMsg] = useState<string | null>(null);
    const [isError, setIsError] = useState(false);

    const [totpSecret, setTotpSecret] = useState<string | null>(null);
    const [totpCode, setTotpCode] = useState("");
    const [disablePassword, setDisablePassword] = useState("");

    async function startTotpSetup() {
        setLoading(true);
        setMsg(null);
        setIsError(false);

        try {
            const data = await setupTwoFactor("TOTP");
            setTotpSecret(data?.secret || null);
            setMsg("Setup TOTP dimulai. Masukkan key ke aplikasi authenticator lalu konfirmasi.");
        } catch (err: unknown) {
            const message = err instanceof Error ? err.message : "Gagal memulai setup TOTP.";
            setIsError(true);
            setMsg(message);
        } finally {
            setLoading(false);
        }
    }

    async function confirmTotp() {
        setLoading(true);
        setMsg(null);
        setIsError(false);

        try {
            await confirmTwoFactor(totpCode);
            setTotpCode("");
            setMsg("2FA TOTP berhasil diaktifkan.");
        } catch (err: unknown) {
            const message = err instanceof Error ? err.message : "Gagal konfirmasi TOTP.";
            setIsError(true);
            setMsg(message);
        } finally {
            setLoading(false);
        }
    }

    async function disable2FA() {
        setLoading(true);
        setMsg(null);
        setIsError(false);

        try {
            await disableTwoFactor(disablePassword);
            setDisablePassword("");
            setTotpSecret(null);
            setTotpCode("");
            setMsg("2FA berhasil dinonaktifkan.");
        } catch (err: unknown) {
            const message = err instanceof Error ? err.message : "Gagal menonaktifkan 2FA.";
            setIsError(true);
            setMsg(message);
        } finally {
            setLoading(false);
        }
    }

    return (
        <AuthShell title="Kelola 2FA" subtitle="Atur metode two-factor authentication untuk akun Anda.">
            <div className="space-y-4">
                <button
                    disabled={loading}
                    className="w-full rounded-xl py-4 text-lg font-bold text-[#002447] bg-[#002447]/10 hover:bg-[#002447]/20 transition-all"
                    onClick={startTotpSetup}
                >
                    {loading ? "Memproses..." : "Mulai Setup 2FA TOTP"}
                </button>

                {totpSecret && (
                    <div className="rounded-xl bg-[#002447]/5 border border-[#002447]/10 p-4 space-y-3">
                        <div>
                            <p className="text-sm text-black/60">Key TOTP</p>
                            <p className="font-mono text-sm break-all">{totpSecret}</p>
                        </div>
                        <div>
                            <label className="block text-sm font-medium mb-2 text-[#002447]">Kode TOTP</label>
                            <input
                                className={inputCls}
                                placeholder="Masukkan kode dari authenticator"
                                value={totpCode}
                                onChange={(e) => setTotpCode(e.target.value)}
                                disabled={loading}
                            />
                        </div>
                        <button disabled={loading || !totpCode} className={buttonCls} onClick={confirmTotp}>
                            Konfirmasi TOTP
                        </button>
                    </div>
                )}

                <div className="rounded-xl border border-red-200 bg-red-50 p-4 space-y-3">
                    <p className="text-sm font-semibold text-red-700">Nonaktifkan 2FA</p>
                    <input
                        className={inputCls}
                        type="password"
                        placeholder="Masukkan password akun"
                        value={disablePassword}
                        onChange={(e) => setDisablePassword(e.target.value)}
                        disabled={loading}
                    />
                    <button
                        disabled={loading || !disablePassword}
                        className="w-full rounded-xl py-3 text-sm font-semibold text-white bg-red-600 hover:bg-red-700"
                        onClick={disable2FA}
                    >
                        Nonaktifkan 2FA
                    </button>
                </div>

                <button
                    type="button"
                    disabled={loading}
                    onClick={() => router.push("/me")}
                    className="w-full rounded-xl py-3 text-sm font-semibold text-[#002447] bg-[#002447]/10 hover:bg-[#002447]/20"
                >
                    Kembali ke Profil
                </button>

                {msg && (
                    <div
                        className={`rounded-xl border px-4 py-3 text-sm ${
                            isError ? "bg-red-50 border-red-100 text-red-600" : "bg-green-50 border-green-100 text-green-700"
                        }`}
                    >
                        {msg}
                    </div>
                )}
            </div>
        </AuthShell>
    );
}
