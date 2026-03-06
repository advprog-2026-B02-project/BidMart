"use client";

import {useState, useEffect} from "react";
import Link from "next/link";
import {useRouter} from "next/navigation";
import AuthShell from "@/components/AuthShell";
import {buttonCls, inputCls} from "@/components/ui";
import {register as apiRegister} from "@/lib/api";

export default function RegisterPage() {
    const [email, setEmail] = useState("");
    const [pass, setPass] = useState("");
    const [pass2, setPass2] = useState("");
    const [loading, setLoading] = useState(false);
    const [isChecking, setIsChecking] = useState(true);
    const [msg, setMsg] = useState<string | null>(null);
    const [isSuccess, setIsSuccess] = useState(false);
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

        if (pass !== pass2) {
            setMsg("Konfirmasi kata sandi tidak sama.");
            return;
        }

        setLoading(true);
        try {
            await apiRegister(email, pass);
            setIsSuccess(true);
            setMsg("Link verifikasi baru telah dikirim! Silahkan cek email Anda.");
        } catch (err: any) {
            setMsg(err.message);
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
        <AuthShell title="Daftar Akun" subtitle="Buat akun BidMart Anda sekarang!">
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
                        disabled={loading || isSuccess}
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
                        minLength={8}
                        disabled={loading || isSuccess}
                    />
                </div>

                <div>
                    <label className="block text-lg font-medium mb-2 text-[#002447]">
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
                        disabled={loading || isSuccess}
                    />
                </div>

                {!isSuccess && (
                    <button disabled={loading} className={`${buttonCls} ${loading ? 'opacity-70' : ''}`}>
                        {loading ? "Memproses..." : "Daftar"}
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

                <div className="pt-6 border-t border-black/5 text-center text-base text-black/60">
                    Sudah punya akun?{" "}
                    <Link className="text-sky-600 font-medium hover:underline" href="/login">
                        Masuk disini!
                    </Link>
                </div>
            </form>
        </AuthShell>
    );
}