"use client";

import {useState, useEffect} from "react";
import Link from "next/link";
import {useRouter} from "next/navigation";
import AuthShell from "@/components/AuthShell";
import {buttonCls, inputCls} from "@/components/ui";
import {login as apiLogin, verifyTwoFactor as apiVerifyTwoFactor} from "@/lib/api";

type PartialLoginResponse = {
    partialToken: string;
    requires2FA: boolean;
    expiresIn: number;
};

function isPartialResponse(data: any): data is PartialLoginResponse {
    return Boolean(data?.requires2FA && data?.partialToken);
}

export default function LoginPage() {
    const [email, setEmail] = useState("");
    const [pass, setPass] = useState("");
    const [loading, setLoading] = useState(false);
    const [isChecking, setIsChecking] = useState(true);
    const [msg, setMsg] = useState<string | null>(null);
    const [partialToken, setPartialToken] = useState<string | null>(null);
    const [twoFactorCode, setTwoFactorCode] = useState("");
    const router = useRouter();

    useEffect(() => {
        const token = localStorage.getItem("accessToken");
        if (token) {
            router.replace("/me");
        } else {
            setIsChecking(false);
        }
    }, [router]);

    async function onSubmit(e: React.FormEvent) {
        e.preventDefault();
        setMsg(null);
        setLoading(true);

        try {
            const result = await apiLogin(email, pass);
            if (isPartialResponse(result)) {
                setPartialToken(result.partialToken);
                setMsg("Masukkan kode TOTP dari authenticator untuk melanjutkan.");
                return;
            }

            router.push("/me");
        } catch (err: unknown) {
            const message =
                err instanceof Error ? err.message : "Terjadi kesalahan saat masuk.";
            setMsg(message);
        } finally {
            setLoading(false);
        }
    }

    async function onVerifyTwoFactor(e: React.FormEvent) {
        e.preventDefault();
        if (!partialToken) {
            setMsg("Sesi 2FA tidak ditemukan. Silakan login ulang.");
            return;
        }

        setMsg(null);
        setLoading(true);

        try {
            await apiVerifyTwoFactor(partialToken, twoFactorCode);
            router.push("/me");
        } catch (err: unknown) {
            const message =
                err instanceof Error ? err.message : "Verifikasi 2FA gagal.";
            setMsg(message);
        } finally {
            setLoading(false);
        }
    }

    if (isChecking) {
        return (
            <div className="min-h-screen bg-bidcream flex items-center justify-center">
                <div
                    className="animate-spin h-10 w-10 border-4 border-[#002447]/20 border-t-[#002447] rounded-full"></div>
            </div>
        );
    }

    return (
        <AuthShell title="Masuk" subtitle="Selamat datang kembali di BidMart">
            {partialToken ? (
                <form onSubmit={onVerifyTwoFactor} className="space-y-6">
                    <div className="rounded-xl bg-blue-50 border border-blue-100 px-4 py-3 text-sm text-blue-700">
                        Verifikasi 2FA TOTP diperlukan sebelum melanjutkan login.
                    </div>

                    <div>
                        <label className="block text-lg font-medium mb-2 text-[#002447]">Kode Verifikasi</label>
                        <input
                            className={inputCls}
                            placeholder="Masukkan 6 digit kode"
                            value={twoFactorCode}
                            onChange={(e) => setTwoFactorCode(e.target.value)}
                            type="text"
                            inputMode="numeric"
                            required
                            disabled={loading}
                        />
                    </div>

                    <button
                        disabled={loading}
                        className={`${buttonCls} ${loading ? 'opacity-70 cursor-not-allowed' : ''}`}
                    >
                        {loading ? "Memverifikasi..." : "Verifikasi 2FA"}
                    </button>

                    <button
                        type="button"
                        className="w-full rounded-xl py-3 text-sm font-semibold text-[#002447] bg-[#002447]/10 hover:bg-[#002447]/20"
                        onClick={() => {
                            setPartialToken(null);
                            setTwoFactorCode("");
                            setMsg(null);
                        }}
                    >
                        Kembali ke Form Login
                    </button>

                    {msg && (
                        <div
                            className="rounded-xl bg-red-50 border border-red-100 px-4 py-3 text-sm text-red-600 animate-in fade-in slide-in-from-top-1">
                            {msg}
                        </div>
                    )}
                </form>
            ) : (
                <form onSubmit={onSubmit} className="space-y-6">
                    <div>
                        <label className="block text-lg font-medium mb-2 text-[#002447]">Email</label>
                        <input
                            className={inputCls}
                            placeholder="Masukkan email Anda"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            type="email"
                            required
                            disabled={loading}
                        />
                    </div>

                    <div>
                        <label className="block text-lg font-medium mb-2 text-[#002447]">Kata Sandi</label>
                        <input
                            className={inputCls}
                            placeholder="Masukkan kata sandi Anda"
                            value={pass}
                            onChange={(e) => setPass(e.target.value)}
                            type="password"
                            required
                            disabled={loading}
                        />
                    </div>

                    <div className="flex justify-end mt-1">
                        <Link href="/auth/forgot" className="text-sm text-sky-600 hover:underline font-medium">
                            Lupa kata sandi?
                        </Link>
                    </div>

                    <button
                        disabled={loading}
                        className={`${buttonCls} ${loading ? 'opacity-70 cursor-not-allowed' : ''}`}
                    >
                        {loading ? "Memproses..." : "Masuk"}
                    </button>

                    {msg && (
                        <div
                            className="rounded-xl bg-red-50 border border-red-100 px-4 py-3 text-sm text-red-600 animate-in fade-in slide-in-from-top-1">
                            {msg}
                        </div>
                    )}

                    <div className="pt-6 border-t border-black/5 text-center text-base text-black/60">
                        Belum punya akun?{" "}
                        <Link className="text-sky-600 font-medium hover:underline" href="/register">
                            Daftar akun disini!
                        </Link>
                    </div>
                </form>
            )}
        </AuthShell>
    );
}
