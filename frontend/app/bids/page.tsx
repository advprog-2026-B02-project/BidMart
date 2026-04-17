// app/bids/page.tsx

"use client";

import {useEffect, useState} from "react";
import Link from "next/link";
import {getMyBids, BidResponseDTO} from "@/lib/bidding.api";
import {me} from "@/lib/api";

export default function MyBidsPage() {
    const [bids, setBids] = useState<BidResponseDTO[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    useEffect(() => {
        const fetchRiwayat = async () => {
            try {
                const token = localStorage.getItem("accessToken");
                if (!token) {
                    setError("Silakan login terlebih dahulu untuk melihat riwayat.");
                    setLoading(false);
                    return;
                }

                const userData = await me();
                if (!userData || !userData.id) {
                    throw new Error("Gagal mendapatkan ID pengguna. Silakan login ulang.");
                }

                const riwayat = await getMyBids(userData.id, token);
                setBids(riwayat);

            } catch (err: any) {
                setError(err.message || "Terjadi kesalahan saat memuat data.");
            } finally {
                setLoading(false);
            }
        };

        fetchRiwayat();
    }, []);

    if (loading) return <div
        className="min-h-screen flex justify-center items-center bg-[#F5F1E8] text-[#002447] animate-pulse">Memuat
        riwayatmu...</div>;
    if (error) return <div className="min-h-screen flex justify-center items-center bg-[#F5F1E8]"><p
        className="text-red-500 font-bold">{error}</p></div>;

    return (
        <div className="min-h-screen bg-[#F5F1E8] p-6 lg:p-12">
            <div className="mx-auto w-full max-w-4xl">
                <h1 className="text-3xl font-bold text-[#002447] mb-8">Riwayat Penawaran Saya</h1>

                {bids.length === 0 ? (
                    <div className="bg-white rounded-[24px] p-10 text-center shadow-sm border border-black/5">
                        <p className="text-gray-500 text-lg">Kamu belum mengikuti lelang apapun.</p>
                        <Link href="/" className="mt-4 inline-block text-[#002447] font-bold underline">
                            Cari barang sekarang
                        </Link>
                    </div>
                ) : (
                    <div className="grid grid-cols-1 gap-4">
                        {bids.map((bid) => (
                            <div key={bid.id}
                                 className="bg-white rounded-[20px] p-6 shadow-sm border border-black/5 flex flex-col md:flex-row md:items-center justify-between gap-4">
                                <div>
                                    <p className="text-sm text-black/50 mb-1">
                                        Tanggal: {new Date(bid.createdAt).toLocaleDateString("id-ID", {
                                        day: 'numeric',
                                        month: 'long',
                                        year: 'numeric'
                                    })}
                                    </p>
                                    <h3 className="font-mono text-xs text-black/40 mb-2">Auction
                                        ID: {bid.auctionId}</h3>
                                    <p className="text-xl font-bold text-[#002447]">
                                        Rp {bid.amount.toLocaleString("id-ID")}
                                    </p>
                                </div>

                                <div className="flex flex-col md:items-end gap-3">
                                    <span
                                        className="px-3 py-1 bg-black/5 rounded-full text-xs font-semibold text-black/60 w-fit">
                                        Status: {bid.status}
                                    </span>

                                    <Link
                                        href={`/auctions/${bid.auctionId}`}
                                        className="px-6 py-2 bg-[#002447] text-white rounded-full text-sm font-semibold hover:bg-[#001830] transition-colors text-center"
                                    >
                                        Lihat Ruang Lelang
                                    </Link>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}