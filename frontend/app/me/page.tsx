"use client";

import {useEffect, useState} from "react";
import {useRouter} from "next/navigation";
import {logout, me} from "@/lib/api";
import AuthShell from "@/components/AuthShell";

interface UserProfile {
    email: string;
    displayName: string;
    avatarUrl?: string | null;
}

export default function MePage() {
    const [user, setUser] = useState<UserProfile | null>(null);
    const [msg, setMsg] = useState<string>("Memuat sesi...");
    const router = useRouter();

    useEffect(() => {
        (async () => {
            try {
                const data = await me();
                setUser({
                    email: data?.email || data?.principal || "Email tidak ditemukan",
                    displayName: data?.displayName || data?.username || "Pengguna Tanpa Nama",
                    avatarUrl: data?.avatarUrl || null,
                });
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

    const getInitials = (name: string) => {
        return name ? name.charAt(0).toUpperCase() : "?";
    };

    return (
        <AuthShell
            title="Profil Akun"
            subtitle="Informasi akun yang sedang aktif saat ini."
        >
            <div className="flex flex-col space-y-6">

                {msg ? (
                    <div className="text-center text-sm font-medium text-black/50 py-4">
                        {msg}
                    </div>
                ) : (
                    <div
                        className="p-6 rounded-2xl bg-[#002447]/5 border border-[#002447]/10 flex items-center space-x-5">
                        {/* FOTO PROFIL / AVATAR */}
                        <div
                            className="flex-shrink-0 w-16 h-16 rounded-full overflow-hidden bg-[#002447] text-white flex items-center justify-center text-2xl font-bold shadow-inner">
                            {user?.avatarUrl ? (
                                <img
                                    src={user.avatarUrl}
                                    alt="Avatar"
                                    className="w-full h-full object-cover"
                                />
                            ) : (
                                getInitials(user?.displayName || "")
                            )}
                        </div>

                        {/* INFO NAMA & EMAIL */}
                        <div className="flex flex-col overflow-hidden">
                            <p className="text-sm font-medium text-black/40 uppercase tracking-wider mb-1">
                                Terhubung sebagai
                            </p>
                            <h2 className="text-xl font-bold text-[#002447] truncate">
                                {user?.displayName}
                            </h2>
                            <p className="text-sm text-black/60 truncate">
                                {user?.email}
                            </p>
                        </div>
                    </div>
                )}

                <div className="pt-4">
                    <button
                        onClick={() => router.push("/me/edit")}
                        className="w-full mb-3 rounded-xl py-4 text-lg font-bold text-[#002447] bg-[#002447]/10 hover:bg-[#002447]/20 transition-all shadow-sm active:scale-[0.98]"
                        disabled={!!msg}
                    >
                        Edit Profil
                    </button>

                    <button
                        onClick={onLogout}
                        className="w-full rounded-xl py-4 text-lg font-bold text-white bg-red-600 hover:bg-red-700 transition-all shadow-md active:scale-[0.98]"
                    >
                        Keluar dari Sesi
                    </button>
                </div>
            </div>
        </AuthShell>
    );
}