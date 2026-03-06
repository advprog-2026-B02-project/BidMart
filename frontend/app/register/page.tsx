"use client";

import {useState} from "react";
import Link from "next/link";
import AuthShell from "@/components/AuthShell";
import {buttonCls, inputCls} from "@/components/ui";
import {register as apiRegister} from "@/lib/api";

export default function RegisterPage() {
    const [email, setEmail] = useState("");
    const [pass, setPass] = useState("");
    const [pass2, setPass2] = useState("");
    const [loading, setLoading] = useState(false);
    const [msg, setMsg] = useState<string | null>(null);

    async function onSubmit(e: React.FormEvent) {
        e.preventDefault();
        setMsg(null);

        if (pass !== pass2) {
            setMsg("Konfirmasi kata sandi tidak sama.");
            return;
        }

        setLoading(true);
        try {
            await apiRegister(email, pass);
            setMsg("Link verifikasi baru telah dikirim! Silahkan cek email Anda.");
            // JANGAN DIPINDAHIN ke /login PLEASE, user gabisa baca messagenya
        } catch (err: unknown) {
            setMsg(err instanceof Error ? err.message : "Gagal mendaftarkan akun.");
        } finally {
            setLoading(false);
        }
    }

    return (
        <AuthShell title="Daftar Akun" subtitle="Buat akun BidMart Anda sekarang!">
            <form onSubmit={onSubmit} className="space-y-6">
                <div>
                    <label className="block text-lg font-medium mb-2">Email</label>
                    <input
                        className={inputCls}
                        placeholder="Masukkan email Anda"
                        value={email}
                        onChange={(e) => setEmail(e.target.value)}
                        type="email"
                        required
                    />
                </div>

                <div>
                    <label className="block text-lg font-medium mb-2">Kata Sandi</label>
                    <input
                        className={inputCls}
                        placeholder="Masukkan kata sandi Anda"
                        value={pass}
                        onChange={(e) => setPass(e.target.value)}
                        type="password"
                        required
                        minLength={8}
                    />
                </div>

                <div>
                    <label className="block text-lg font-medium mb-2">
                        Konfirmasi Kata Sandi
                    </label>
                    <input
                        className={inputCls}
                        placeholder="Konfirmasi kata sandi Anda"
                        value={pass2}
                        onChange={(e) => setPass2(e.target.value)}
                        type="password"
                        required
                        minLength={8}
                    />
                </div>

                <button disabled={loading} className={buttonCls}>
                    {loading ? "Memproses..." : "Daftar"}
                </button>

                {msg && (
                    <div className="rounded-xl bg-black/5 px-4 py-3 text-sm text-black/70">
                        {msg}
                    </div>
                )}

                <div className="pt-6 border-t text-center text-base">
                    Sudah punya akun?{" "}
                    <Link className="text-sky-600 underline" href="/login">
                        Masuk disini!
                    </Link>
                </div>
            </form>
        </AuthShell>
    );
}
