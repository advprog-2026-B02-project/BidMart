"use client";

import {useEffect, useState} from "react";
import {useRouter} from "next/navigation";
import AuthShell from "@/components/AuthShell";
import {buttonCls, inputCls} from "@/components/ui";
import {me, updateProfile} from "@/lib/api";

export default function EditProfilePage() {
    const router = useRouter();

    const [displayName, setDisplayName] = useState("");
    const [avatarUrl, setAvatarUrl] = useState("");

    const [loading, setLoading] = useState(false);
    const [initLoading, setInitLoading] = useState(true);
    const [msg, setMsg] = useState<string | null>(null);
    const [isSuccess, setIsSuccess] = useState(false);

    // Ambil data profile saat ini biar form-nya langsung keisi
    useEffect(() => {
        (async () => {
            try {
                const data = await me();
                setDisplayName(data?.displayName || "");
                setAvatarUrl(data?.avatarUrl || "");
            } catch (err: any) {
                setMsg(err?.message || "Gagal memuat profil.");
            } finally {
                setInitLoading(false);
            }
        })();
    }, []);

    async function onSubmit(e: React.FormEvent) {
        e.preventDefault();
        setMsg(null);
        setLoading(true);

        try {
            await updateProfile(displayName, avatarUrl);
            setIsSuccess(true);
            setMsg("Profil berhasil diperbarui! Mengalihkan...");

            // Balik ke halaman /me setelah 2 detik
            setTimeout(() => {
                router.push("/me");
            }, 2000);
        } catch (err: any) {
            setMsg(err.message);
        } finally {
            setLoading(false);
        }
    }

    if (initLoading) {
        return (
            <AuthShell title="Edit Profil" subtitle="Memuat data Anda...">
                <div className="text-center p-10 text-[#002447]">Memuat...</div>
            </AuthShell>
        );
    }

    return (
        <AuthShell title="Edit Profil" subtitle="Perbarui nama dan foto profil Anda.">
            {!isSuccess ? (
                <form onSubmit={onSubmit} className="space-y-6 animate-in fade-in duration-300">
                    <div>
                        <label className="block text-lg font-medium mb-2 text-[#002447]">Nama Tampilan</label>
                        <input
                            className={inputCls}
                            type="text"
                            placeholder="Contoh: John Doe"
                            value={displayName}
                            onChange={(e) => setDisplayName(e.target.value)}
                            required
                            maxLength={100}
                            disabled={loading}
                        />
                    </div>

                    <div>
                        <label className="block text-lg font-medium mb-2 text-[#002447]">URL Foto Profil
                            (Opsional)</label>
                        <input
                            className={inputCls}
                            type="url"
                            placeholder="https://contoh.com/foto.jpg"
                            value={avatarUrl}
                            onChange={(e) => setAvatarUrl(e.target.value)}
                            disabled={loading}
                        />
                        <p className="mt-2 text-xs text-black/50">
                            Masukkan link gambar (URL). Kosongkan jika tidak ingin menggunakan foto.
                        </p>
                    </div>

                    <div className="flex flex-col space-y-3 pt-4">
                        <button disabled={loading} className={buttonCls}>
                            {loading ? "Menyimpan..." : "Simpan Perubahan"}
                        </button>

                        <button
                            type="button"
                            disabled={loading}
                            onClick={() => router.push("/me")}
                            className="text-center text-sm font-medium text-black/50 hover:text-[#002447] transition-colors"
                        >
                            Batal
                        </button>
                    </div>

                    {msg && (
                        <div className="rounded-xl bg-red-50 text-red-600 border border-red-100 px-4 py-3 text-sm">
                            {msg}
                        </div>
                    )}
                </form>
            ) : (
                <div className="space-y-6 animate-in fade-in zoom-in duration-300 text-center">
                    <div
                        className="rounded-xl bg-green-50 text-green-700 border border-green-100 px-4 py-4 text-sm font-medium leading-relaxed">
                        {msg}
                    </div>
                </div>
            )}
        </AuthShell>
    );
}