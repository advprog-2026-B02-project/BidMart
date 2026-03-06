"use client";

import {useState} from "react";
import {useRouter} from "next/navigation";
import AuthShell from "@/components/AuthShell"; // Sesuaikan path
import {buttonCls, inputCls} from "@/components/ui"; // Sesuaikan path
import {register} from "@/lib/api";

export default function RegisterPage() {
    const router = useRouter();

    // State form
    const [displayName, setDisplayName] = useState(""); // State baru
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");

    // State UI
    const [loading, setLoading] = useState(false);
    const [msg, setMsg] = useState<string | null>(null);
    const [isSuccess, setIsSuccess] = useState(false);

    async function onSubmit(e: React.FormEvent) {
        e.preventDefault();
        setMsg(null);
        setLoading(true);

        try {
            // Oper displayName ke fungsi API
            await register(email, password, displayName);
            setIsSuccess(true);
            setMsg("Registrasi berhasil! Silakan cek email Anda untuk verifikasi.");
        } catch (err: any) {
            setMsg(err.message);
        } finally {
            setLoading(false);
        }
    }

    return (
        <AuthShell title="Buat Akun Baru" subtitle="Bergabunglah dengan BidMart sekarang.">
            {!isSuccess ? (
                <form onSubmit={onSubmit} className="space-y-6 animate-in fade-in duration-300">

                    {/* INPUT NAMA TAMPILAN */}
                    <div>
                        <label className="block text-lg font-medium mb-2 text-[#002447]">Nama Tampilan</label>
                        <input
                            className={inputCls}
                            type="text"
                            placeholder="Masukkan nama Anda (mis: John Doe)"
                            value={displayName}
                            onChange={(e) => setDisplayName(e.target.value)}
                            required
                            maxLength={100}
                            disabled={loading}
                        />
                    </div>

                    {/* INPUT EMAIL */}
                    <div>
                        <label className="block text-lg font-medium mb-2 text-[#002447]">Email</label>
                        <input
                            className={inputCls}
                            type="email"
                            placeholder="Masukkan email aktif"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            disabled={loading}
                        />
                    </div>

                    {/* INPUT PASSWORD */}
                    <div>
                        <label className="block text-lg font-medium mb-2 text-[#002447]">Kata Sandi</label>
                        <input
                            className={inputCls}
                            type="password"
                            placeholder="Minimal 8 karakter"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            minLength={8}
                            disabled={loading}
                        />
                    </div>

                    <button disabled={loading} className={buttonCls}>
                        {loading ? "Mendaftar..." : "Daftar Sekarang"}
                    </button>

                    {msg && (
                        <div className="rounded-xl bg-red-50 text-red-600 border border-red-100 px-4 py-3 text-sm">
                            {msg}
                        </div>
                    )}

                    <div className="mt-4 text-center">
                        <span className="text-sm text-black/60">Sudah punya akun? </span>
                        <button
                            type="button"
                            onClick={() => router.push("/login")}
                            className="text-sm text-[#002447] font-medium hover:underline underline-offset-4"
                        >
                            Masuk di sini
                        </button>
                    </div>
                </form>
            ) : (
                <div className="space-y-6 animate-in fade-in zoom-in duration-300 text-center">
                    <div
                        className="rounded-xl bg-green-50 text-green-700 border border-green-100 px-4 py-4 text-sm font-medium leading-relaxed">
                        {msg}
                    </div>
                    <button onClick={() => router.push("/login")} className={`${buttonCls} bg-[#002447] w-full`}>
                        Menuju Halaman Login
                    </button>
                </div>
            )}
        </AuthShell>
    );
}