// app/auctions/[id]/page.tsx

"use client";

import {useEffect, useState} from "react";
import {useParams} from "next/navigation";
import {getAuctionStatus, placeBid, AuctionResponseDTO} from "@/lib/bidding.api";
import {useBiddingWebSocket} from "@/hooks/useBiddingWebSocket";
import {inputCls, buttonCls} from "@/components/ui";

export default function BiddingRoomPage() {
    const params = useParams();
    const id = params.id as string;

    const [auction, setAuction] = useState<AuctionResponseDTO | null>(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    // state untuk timer dan input bid
    const [timeLeft, setTimeLeft] = useState<string>("");
    const [bidAmount, setBidAmount] = useState<string>("");
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [bidError, setBidError] = useState("");

    const {bids, auctionStatus, isConnected} = useBiddingWebSocket({auctionId: id});

    // ambil data lelang saat inisialisasi
    useEffect(() => {
        if (!id) return;
        getAuctionStatus(id)
            .then((data) => {
                setAuction(data);
                setLoading(false);
            })
            .catch((err) => {
                setError(err.message);
                setLoading(false);
            });
    }, [id]);

    // kalkulasi hitung mundur waktu lelang
    useEffect(() => {
        if (!auction?.endTime) return;

        const timer = setInterval(() => {
            const difference = new Date(auction.endTime).getTime() - new Date().getTime();

            if (difference <= 0) {
                setTimeLeft("Waktu Habis");
                clearInterval(timer);
            } else {
                const hours = Math.floor((difference / (1000 * 60 * 60)) % 24);
                const minutes = Math.floor((difference / 1000 / 60) % 60);
                const seconds = Math.floor((difference / 1000) % 60);
                setTimeLeft(`${hours}j ${minutes}m ${seconds}d`);
            }
        }, 1000);

        return () => clearInterval(timer);
    }, [auction?.endTime]);

    // proses pengiriman penawaran lelang
    const handlePlaceBid = async (e: React.FormEvent) => {
        e.preventDefault();
        setBidError("");
        const amountNumber = parseInt(bidAmount.replace(/\D/g, ""));

        if (!amountNumber || amountNumber <= 0) {
            setBidError("Masukkan nominal yang valid");
            return;
        }

        setIsSubmitting(true);
        try {
            // buat key idempotency menggunakan timestamp
            const idempotencyKey = `bid-${Date.now()}`;
            await placeBid(id, amountNumber, idempotencyKey);
            setBidAmount("");
        } catch (err: any) {
            setBidError(err.message || "gagal melakukan penawaran");
        } finally {
            setIsSubmitting(false);
        }
    };

    // validasi status error
    if (loading) return <div className="min-h-screen flex justify-center items-center bg-[#F5F1E8]">
        <div className="text-2xl font-semibold text-[#002447] animate-pulse">Memuat Bidding Room...</div>
    </div>;
    if (error) return <div className="min-h-screen flex justify-center items-center bg-[#F5F1E8]">
        <div className="p-8 bg-white rounded-3xl shadow-sm border border-red-100 text-center"><h2
            className="text-2xl font-bold text-red-600 mb-2">Waduh, Error!</h2><p className="text-gray-600">{error}</p>
        </div>
    </div>;

    // gunakan harga dari websocket jika tersedia
    const currentHighestPrice = bids.length > 0 ? bids[0].amount : auction?.currentPrice;
    const finalStatus = auctionStatus || auction?.status;

    return (
        <div className="min-h-screen bg-[#F5F1E8] p-6 lg:p-12">
            <div
                className="mx-auto w-full max-w-5xl rounded-[28px] bg-white border border-black/5 shadow-sm p-8 lg:p-10 text-black">

                {/* header status koneksi dan informasi */}
                <div className="flex justify-between items-center mb-8 border-b border-gray-100 pb-6">
                    <div>
                        <h1 className="text-3xl lg:text-4xl font-semibold text-[#002447]">Live Bidding Room</h1>
                        <p className="mt-2 text-black/60 font-mono text-sm">ID: {auction?.listingId}</p>
                    </div>
                    <div
                        className={`px-4 py-2 rounded-full text-sm font-semibold flex items-center gap-2 ${isConnected ? "bg-green-50 text-green-700" : "bg-red-50 text-red-700"}`}>
                        <div
                            className={`w-2 h-2 rounded-full ${isConnected ? "bg-green-500 animate-pulse" : "bg-red-500"}`}></div>
                        {isConnected ? "Live" : "Terputus"}
                    </div>
                </div>

                <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">

                    {/* kolom status lelang dan timer */}
                    <div className="lg:col-span-7 flex flex-col gap-6">
                        <div className="bg-black/5 rounded-[24px] p-8 text-center">
                            <p className="text-lg text-black/60 mb-2">Harga Terkini</p>
                            <h2 className="text-5xl font-bold text-[#002447]">
                                Rp {currentHighestPrice?.toLocaleString("id-ID")}
                            </h2>
                            <p className="mt-4 text-sm text-black/50">
                                Minimal Bid Selanjutnya:
                                Rp {(currentHighestPrice! + (auction?.minimumNextBid || 0)).toLocaleString("id-ID")}
                            </p>
                        </div>

                        <div className="grid grid-cols-2 gap-4">
                            <div className="bg-white border border-black/10 rounded-[20px] p-6 text-center shadow-sm">
                                <p className="text-sm text-black/60 mb-1">Sisa Waktu</p>
                                <p className={`text-2xl font-bold ${timeLeft === "Waktu Habis" ? "text-red-500" : "text-[#002447]"}`}>
                                    {timeLeft || "Menghitung..."}
                                </p>
                            </div>
                            <div className="bg-white border border-black/10 rounded-[20px] p-6 text-center shadow-sm">
                                <p className="text-sm text-black/60 mb-1">Status</p>
                                <p className="text-2xl font-bold text-[#002447]">
                                    {finalStatus === "ACTIVE" ? "🟢 Berlangsung" :
                                        finalStatus === "EXTENDED" ? "🟡 Diperpanjang" :
                                            finalStatus === "WON" ? "🎉 Terjual" :
                                                finalStatus === "UNSOLD" ? "❌ Gagal Terjual" : finalStatus}
                                </p>
                            </div>
                        </div>
                    </div>

                    {/* kolom form penawaran dan riwayat */}
                    <div className="lg:col-span-5 flex flex-col h-[500px]">
                        <h2 className="text-xl font-semibold text-[#002447] mb-4">Riwayat Penawaran</h2>

                        {/* daftar riwayat penawaran */}
                        <div className="flex-1 bg-black/5 rounded-[24px] p-4 overflow-y-auto mb-4 flex flex-col gap-3">
                            {bids.length === 0 ? (
                                <div className="h-full flex items-center justify-center text-black/40 italic">
                                    Belum ada penawaran. Jadilah yang pertama!
                                </div>
                            ) : (
                                bids.map((bid, index) => (
                                    <div key={bid.id}
                                         className={`p-4 rounded-xl flex justify-between items-center ${index === 0 ? "bg-[#002447] text-white" : "bg-white border border-black/5"}`}>
                                        <div>
                                            <p className={`text-sm font-semibold ${index === 0 ? "text-white" : "text-[#002447]"}`}>
                                                {bid.bidderId} {index === 0 && "(Tertinggi)"}
                                            </p>
                                            <p className={`text-xs ${index === 0 ? "text-white/70" : "text-black/50"}`}>
                                                {new Date(bid.createdAt).toLocaleTimeString()}
                                            </p>
                                        </div>
                                        <p className="font-bold">Rp {bid.amount.toLocaleString("id-ID")}</p>
                                    </div>
                                ))
                            )}
                        </div>

                        {/* form input penawaran */}
                        <form onSubmit={handlePlaceBid} className="space-y-3 mt-auto">
                            {bidError && <p className="text-red-500 text-sm text-center">{bidError}</p>}
                            <input
                                type="number"
                                placeholder="Masukkan nominal (Misal: 1500000)"
                                className={inputCls}
                                value={bidAmount}
                                onChange={(e) => setBidAmount(e.target.value)}
                                disabled={finalStatus !== "ACTIVE" && finalStatus !== "EXTENDED"}
                            />
                            <button
                                type="submit"
                                className={buttonCls}
                                disabled={isSubmitting || (finalStatus !== "ACTIVE" && finalStatus !== "EXTENDED")}
                            >
                                {isSubmitting ? "Mengirim..." : "Kirim Penawaran"}
                            </button>
                        </form>
                    </div>

                </div>
            </div>
        </div>
    );
}