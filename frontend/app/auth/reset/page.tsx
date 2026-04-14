"use client";

import {useState, useEffect, Suspense} from "react";
import {useSearchParams, useRouter} from "next/navigation";
import AuthShell from "@/components/AuthShell";
import {buttonCls, inputCls} from "@/components/ui";
import {resetPassword, validateResetToken} from "@/lib/api";

function ResetContent() {
    const searchParams = useSearchParams();
    const router = useRouter();
    const token = searchParams.get("token");
    const hasToken = Boolean(token);
    const missingTokenMessage = "Token tidak valid atau tidak ditemukan di URL.";

    const [pass, setPass] = useState("");
    const [pass2, setPass2] = useState("");
    const [loading, setLoading] = useState(false);
    const [msg, setMsg] = useState<string | null>(hasToken ? null : missingTokenMessage);
    const [isSuccess, setIsSuccess] = useState(false);

    const [tokenStatus, setTokenStatus] = useState<"checking" | "valid" | "invalid">(
        hasToken ? "checking" : "invalid",
    );

    useEffect(() => {
        if (!token) {
            return;
        }

        let isActive = true;

        async function checkToken() {
            try {
                await validateResetToken(token);
                if (!isActive) {
                    return;
                }
                setTokenStatus("valid");
            } catch (err: unknown) {
                if (!isActive) {
                    return;
                }
                const message =
                    err instanceof Error ? err.message : "Token reset tidak valid.";
                setMsg(message);
                setTokenStatus("invalid");
            }
        }

        checkToken();

        return () => {
            isActive = false;
        };
    }, [token]);

    async function onSubmit(e: React.FormEvent) {
        e.preventDefault();
        setMsg(null);

        if (pass !== pass2) {
            setMsg("Konfirmasi kata sandi tidak cocok.");
            return;
        }

        if (!token) {
            setMsg(missingTokenMessage);
            setTokenStatus("invalid");
            return;
        }

        setLoading(true);
        try {
            await resetPassword(token, pass);
            setIsSuccess(true);
            setMsg("Kata sandi berhasil diperbarui! Mengalihkan ke halaman login...");

            setTimeout(() => {
                router.push("/login");
            }, 2000);

        } catch (err: unknown) {
            const message =
                err instanceof Error ? err.message : "Gagal memperbarui kata sandi.";
            setMsg(message);
            setTokenStatus("invalid");
        } finally {
            setLoading(false);
        }
    }

    if (tokenStatus === "checking") {
        return (
            <div className="flex flex-col items-center justify-center py-10 space-y-4 animate-in fade-in">
                <div
                    className="h-8 w-8 animate-spin rounded-full border-4 border-[#002447]/20 border-t-[#002447]"></div>
                <p className="text-sm font-medium text-black/60">Memverifikasi link...</p>
            </div>
        );
    }

    if (tokenStatus === "invalid") {
        return (
            <div className="text-center space-y-6 animate-in fade-in zoom-in duration-300">
                <div
                    className="rounded-xl bg-red-50 text-red-600 border border-red-100 px-4 py-4 text-sm font-medium leading-relaxed">
                    {msg}
                </div>
                <button
                    onClick={() => router.push("/login")}
                    className={`${buttonCls} bg-[#002447] w-full`}
                >
                    Kembali ke Login
                </button>
            </div>
        );
    }

    return (
        <form onSubmit={onSubmit} className="space-y-6 animate-in fade-in duration-300">
            <div>
                <label className="block text-lg font-medium mb-2 text-[#002447]">Kata Sandi Baru</label>
                <input
                    className={inputCls}
                    type="password"
                    placeholder="Masukkan kata sandi baru"
                    value={pass}
                    onChange={(e) => setPass(e.target.value)}
                    required
                    minLength={8}
                    disabled={loading || isSuccess}
                />
            </div>

            <div>
                <label className="block text-lg font-medium mb-2 text-[#002447]">Konfirmasi Kata Sandi</label>
                <input
                    className={inputCls}
                    type="password"
                    placeholder="Ulangi kata sandi baru"
                    value={pass2}
                    onChange={(e) => setPass2(e.target.value)}
                    required
                    minLength={8}
                    disabled={loading || isSuccess}
                />
            </div>

            {!isSuccess && (
                <button disabled={loading} className={buttonCls}>
                    {loading ? "Memproses..." : "Update Kata Sandi"}
                </button>
            )}

            {msg && (
                <div className={`rounded-xl px-4 py-3 text-sm animate-in fade-in slide-in-from-top-1 ${
                    isSuccess
                        ? "bg-green-50 border border-green-100 text-green-700 font-medium"
                        : "bg-red-50 border border-red-100 text-red-600"
                }`}>
                    {msg}
                </div>
            )}
        </form>
    );
}

export default function ResetPage() {
    return (
        <AuthShell title="Setel Ulang Sandi" subtitle="Masukkan kata sandi baru untuk akun Anda.">
            <Suspense fallback={<div className="text-center p-10 text-[#002447]">Memuat...</div>}>
                <ResetContent/>
            </Suspense>
        </AuthShell>
    );
}
