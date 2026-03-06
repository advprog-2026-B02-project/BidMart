"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { logout, me } from "@/lib/api";

export default function MePage() {
    const [principal, setPrincipal] = useState<string | null>(null);
    const [msg, setMsg] = useState<string>("Memuat sesi...");
    const router = useRouter();

    useEffect(() => {
        (async () => {
            try {
                const data = await me();
                setPrincipal(data?.principal ?? "User");
                setMsg("");
            } catch (err: any) {
                setMsg(err?.message || "Belum login / sesi habis.");
            }
        })();
    }, []);

    async function onLogout() {
        await logout();
        router.push("/login");
    }

    return (
        <div className="min-h-screen bg-bidcream flex items-center justify-center p-6">
            <div className="w-full max-w-xl rounded-[28px] bg-white border border-black/5 shadow-sm px-10 py-10">
                <div className="text-3xl font-semibold">Session</div>
                <div className="mt-3 text-black/70">
                    {principal ? `Logged in as: ${principal}` : msg}
                </div>

                <button
                    onClick={onLogout}
                    className="mt-8 w-full rounded-xl py-3 text-xl font-semibold text-white bg-gradient-to-r from-bidnavy to-bidnavy2"
                >
                    Logout
                </button>
            </div>
        </div>
    );
}