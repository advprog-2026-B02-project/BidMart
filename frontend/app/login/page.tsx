"use client";

import {useState} from "react";
import Link from "next/link";
import {useRouter} from "next/navigation";
import AuthShell from "@/components/AuthShell";
import {buttonCls, inputCls} from "@/components/ui";
import {login as apiLogin} from "@/lib/api";

export default function LoginPage() {
    const [email, setEmail] = useState("");
    const [pass, setPass] = useState("");
    const [loading, setLoading] = useState(false);
    const [msg, setMsg] = useState<string | null>(null);
    const router = useRouter();

    async function onSubmit(e: React.FormEvent) {
        e.preventDefault();
        setMsg(null);
        setLoading(true);

        try {
            await apiLogin(email, pass);
            router.push("/me");
        } catch (err: any) {
            setMsg(err.message);
        } finally {
            setLoading(false);
        }
    }

    return (
        <AuthShell title="Masuk" subtitle="Selamat datang kembali di BidMart">
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
                    />
                </div>

                <button disabled={loading} className={buttonCls}>
                    {loading ? "Memproses..." : "Masuk"}
                </button>

                {msg && (
                    <div className="rounded-xl bg-black/5 px-4 py-3 text-sm text-black/70">
                        {msg}
                    </div>
                )}

                <div className="pt-6 border-t text-center text-base">
                    Belum punya akun?{" "}
                    <Link className="text-sky-600 underline" href="/register">
                        Daftar akun disini!
                    </Link>
                </div>
            </form>
        </AuthShell>
    );
}