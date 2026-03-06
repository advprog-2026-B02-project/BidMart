"use client";

import {useEffect, useState} from "react";
import {useRouter} from "next/navigation";
import {logout, me} from "@/lib/api";
import AuthShell from "@/components/AuthShell";

export default function MePage() {
    const [principal, setPrincipal] = useState<string | null>(null);
    const [msg, setMsg] = useState<string>("Memuat sesi...");
    const router = useRouter();

    useEffect(() => {
        (async () => {
            try {
                const data = await me();
                const userDisplayName = data?.email || data?.username || data?.principal || "User";
                setPrincipal(userDisplayName);
                setMsg("");
            } catch (err: any) {
                setMsg(err?.message || "Belum login / sesi habis.");
            }
        })();
    }, []);

    async function onLogout() {
        try {
            await logout();
            router.push("/login");
        } catch (err) {
            // do nothing
        }
    }

    return (
        <AuthShell
            title="Sesi Akun"
            subtitle="Informasi akun yang sedang aktif saat ini."
        >
            <div className="flex flex-col space-y-6">
                <div className="p-6 rounded-2xl bg-[#002447]/5 border border-[#002447]/10">
                    <p className="text-sm font-medium text-black/40 uppercase tracking-wider">
                        Terhubung sebagai
                    </p>
                    <p className="mt-1 text-2xl font-bold text-[#002447]">
                        {principal || msg}
                    </p>
                </div>

                <div className="pt-4">
                    <button
                        onClick={onLogout}
                        className="w-full rounded-xl py-4 text-lg font-bold text-white bg-[#002447] hover:bg-[#002447]/90 transition-all shadow-md active:scale-[0.98]"
                    >
                        Keluar dari Sesi
                    </button>

                    <p className="mt-4 text-center text-sm text-black/50">
                        Bukan akun Anda? Silakan keluar dan masuk kembali.
                    </p>
                </div>
            </div>
        </AuthShell>
    );
}