"use client";

import {useState, useEffect} from "react";
import Link from "next/link";
import {useRouter} from "next/navigation";
import AuthShell from "@/components/AuthShell";
import {buttonCls, inputCls} from "@/components/ui";
import {login as apiLogin} from "@/lib/api";

export default function LoginPage() {
    const [email, setEmail] = useState("");
    const [pass, setPass] = useState("");
    const [loading, setLoading] = useState(false);
    const [isChecking, setIsChecking] = useState(true);
    const [msg, setMsg] = useState<string | null>(null);
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
            await apiLogin(email, pass);
            router.push("/me");
        } catch (err: any) {
            setMsg(err.message);
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
        </AuthShell>
    );
}