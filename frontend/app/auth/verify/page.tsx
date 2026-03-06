"use client";

import {useEffect, useState, Suspense} from "react";
import {useSearchParams, useRouter} from "next/navigation";
import AuthShell from "@/components/AuthShell";
import {buttonCls} from "@/components/ui";
import {verifyEmail} from "@/lib/api";

function VerifyContent() {
    const searchParams = useSearchParams();
    const router = useRouter();
    const token = searchParams.get("token");

    const [status, setStatus] = useState<"loading" | "success" | "error">("loading");
    const [msg, setMsg] = useState("");

    useEffect(() => {
        if (!token) {
            setStatus("error");
            setMsg("Token verifikasi tidak ditemukan.");
            return;
        }

        async function doVerify() {
            try {
                await verifyEmail(token!);
                setStatus("success");
            } catch (err: any) {
                setStatus("error");
                setMsg(err.message);
            }
        }

        doVerify();
    }, [token]);

    return (
        <div className="flex flex-col items-center justify-center space-y-8 py-4">
            {status === "loading" && (
                <div className="flex flex-col items-center space-y-4">
                    <div
                        className="animate-spin h-10 w-10 border-4 border-[#002447]/20 border-t-[#002447] rounded-full"></div>
                    <p className="text-lg text-[#002447]/60 font-medium">Memproses verifikasi Anda...</p>
                </div>
            )}

            {status === "success" && (
                <div className="text-center space-y-6 animate-in fade-in zoom-in duration-500">
                    <div className="flex justify-center">
                        <div className="bg-green-100 p-4 rounded-full">
                            <svg className="w-12 h-12 text-green-600" fill="none" stroke="currentColor"
                                 viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
                                      d="M5 13l4 4L19 7"></path>
                            </svg>
                        </div>
                    </div>
                    <div className="space-y-2">
                        <h2 className="text-2xl font-bold text-[#002447]">Verifikasi Berhasil!</h2>
                        <p className="text-black/60 text-base">Akun Anda telah aktif. Selamat bergabung di BidMart.</p>
                    </div>
                    <button onClick={() => router.push("/login")} className={buttonCls + " w-full"}>
                        Masuk Sekarang
                    </button>
                </div>
            )}

            {status === "error" && (
                <div className="text-center space-y-6 animate-in fade-in zoom-in duration-500">
                    <div className="flex justify-center">
                        <div className="bg-red-100 p-4 rounded-full">
                            <svg className="w-12 h-12 text-red-600" fill="none" stroke="currentColor"
                                 viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2"
                                      d="M6 18L18 6M6 6l12 12"></path>
                            </svg>
                        </div>
                    </div>
                    <div className="space-y-2">
                        <h2 className="text-2xl font-bold text-[#002447]">Verifikasi Gagal</h2>
                        <div className="rounded-xl bg-black/5 px-4 py-3 text-sm text-black/70">
                            {msg}
                        </div>
                    </div>
                    <button onClick={() => router.push("/register")} className={buttonCls + " w-full"}>
                        Kembali ke Daftar
                    </button>
                </div>
            )}
        </div>
    );
}

export default function VerifyPage() {
    return (
        <AuthShell title="Aktivasi Akun" subtitle="Hanya satu langkah lagi untuk memulai.">
            <Suspense fallback={<div className="text-center p-10 text-[#002447]">Memuat...</div>}>
                <VerifyContent/>
            </Suspense>
        </AuthShell>
    );
}