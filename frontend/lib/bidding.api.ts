// lib/bidding.api.ts

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";

// types

export interface BidResponseDTO {
    id: string;
    auctionId: string;
    bidderId: string;
    amount: number;
    status: string;
    previousHighBid?: number;
    isNewHighBid: boolean;
    createdAt: string;
}

export interface AuctionResponseDTO {
    id: string;
    listingId: string;
    status: string;
    currentPrice: number;
    minimumNextBid: number;
    highestBidderId?: string;
    startTime: string;
    endTime: string;
    extensionCount: number;
    reserveMet: boolean;
}

export interface Page<T> {
    content: T[];
    totalPages: number;
    totalElements: number;
    size: number;
    number: number;
}

// utils

function getAuthHeader(): Record<string, string> {
    if (typeof window === "undefined") return {};
    const token = localStorage.getItem("accessToken");
    return token ? {"Authorization": `Bearer ${token}`} : {};
}

// api functions

export async function getAuctionStatus(auctionId: string): Promise<AuctionResponseDTO> {
    const res = await fetch(`${BASE_URL}/auctions/${auctionId}`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
            ...getAuthHeader(),
        },
    });

    if (!res.ok) throw new Error("gagal mengambil status lelang");
    return res.json();
}

export async function getBidHistory(auctionId: string, page = 0, size = 10): Promise<Page<BidResponseDTO>> {
    const res = await fetch(`${BASE_URL}/auctions/${auctionId}/bids?page=${page}&size=${size}`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
            ...getAuthHeader(),
        },
    });

    if (!res.ok) throw new Error("gagal mengambil riwayat penawaran");
    return res.json();
}

export async function placeBid(auctionId: string, amount: number, idempotencyKey?: string): Promise<BidResponseDTO> {
    const headers: Record<string, string> = {
        "Content-Type": "application/json",
        ...getAuthHeader(),
    };

    // tambahkan idempotency-key jika ada (buat cegah double submit)
    if (idempotencyKey) {
        headers["Idempotency-Key"] = idempotencyKey;
    }

    const res = await fetch(`${BASE_URL}/auctions/${auctionId}/bids`, {
        method: "POST",
        headers,
        body: JSON.stringify({amount}),
    });

    if (!res.ok) {
        // handle error return dari backend (ex: "penawaran terlalu rendah")
        const errText = await res.text();
        throw new Error(errText || "gagal melakukan penawaran");
    }

    return res.json();
}

export async function startAuction(listingId: string, payload: {
    startPrice: number;
    minimumIncrement: number;
    reservePrice: number;
    endTime: string
}): Promise<AuctionResponseDTO> {
    const res = await fetch(`${BASE_URL}/auctions/listings/${listingId}/start`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            ...getAuthHeader(),
        },
        body: JSON.stringify(payload),
    });

    if (!res.ok) throw new Error("gagal memulai lelang");
    return res.json();
}

export const getMyBids = async (userId: string, token: string) => {
    // Sesuaikan BASE_URL dengan yang lu pake
    const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

    const res = await fetch(`${BASE_URL}/auctions/my-bids?page=0&size=20`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
            "Authorization": `Bearer ${token}`,
            "X-User-Id": userId // Sesuai dengan @RequestHeader di controller lu
        },
    });

    if (!res.ok) {
        throw new Error("Gagal mengambil riwayat penawaran");
    }

    const data = await res.json();

    return data.content as BidResponseDTO[];
};