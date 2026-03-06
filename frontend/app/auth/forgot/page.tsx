"use client";

import {useState} from "react";
import {useRouter} from "next/navigation";
import AuthShell from "@/components/AuthShell";
import {buttonCls, inputCls} from "@/components/ui";
import {forgotPassword} from "@/lib/api";

export default function ForgotPage() {
    const [email, setEmail] = useState("");
    const [loading, setLoading] = useState(false);
    const [msg, setMsg] = useState<string | null>(null);
    const [isSuccess, setIsSuccess] = useState(false);
    const router = useRouter();

    async function onSubmit(e: React.FormEvent) {
        e.preventDefault();
        setLoading(true);
        setMsg(null);

        try {
            await forgotPassword(email);
            setIsSuccess(true);
            setMsg("Instruksi reset kata sandi telah dikirim ke email Anda. Silakan cek kotak masuk atau folder spam.");
        } catch (err: any) {
            setMsg(err.message);
        } finally {
            setLoading(false);
        }
    }

    return (
        <AuthShell title="Lupa Kata Sandi" subtitle="Kami akan mengirimkan link reset ke email Anda.">
            <div className="flex flex-col">
                {!isSuccess ? (
                    <>
                        <form onSubmit={onSubmit} className="space-y-6">
                            <div>
                                <label className="block text-lg font-medium mb-2 text-[#002447]">Email</label>
                                <input
                                    className={inputCls}
                                    type="email"
                                    placeholder="Masukkan email terdaftar"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    required
                                    disabled={loading}
                                />
                            </div>

                            <button disabled={loading} className={buttonCls}>
                                {loading ? "Mengirim..." : "Kirim Link Reset"}
                            </button>
                        </form>

                        {/* Pindah ke sini biar nempel di bawah tombol, bukan di bawah container */}
                        <div className="mt-4 text-center">
                            <button
                                onClick={() => router.push("/login")}
                                className="text-sm text-black/40 hover:text-[#002447] underline underline-offset-4 transition-colors"
                            >
                                Kembali
                            </button>
                        </div>
                    </>
                ) : (
                    <div className="space-y-6 animate-in fade-in zoom-in duration-300">
                        <div
                            className="rounded-xl bg-green-50 text-green-700 border border-green-100 px-4 py-4 text-sm font-medium leading-relaxed">
                            {msg}
                        </div>

                        <button
                            onClick={() => router.push("/login")}
                            className={`${buttonCls} bg-[#002447]`}
                        >
                            Ke Halaman Login
                        </button>
                    </div>
                )}

                {msg && !isSuccess && (
                    <div
                        className="mt-6 rounded-xl bg-red-50 text-red-600 border border-red-100 px-4 py-3 text-sm animate-in fade-in slide-in-from-top-1">
                        {msg}
                    </div>
                )}
            </div>
        </AuthShell>
    );
}