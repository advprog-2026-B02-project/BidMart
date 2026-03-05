"use client";

import Link from "next/link";
import {useEffect, useState} from "react";

export default function Home() {
    const [views, setViews] = useState(0);

    useEffect(() => {
        fetch("http://localhost:8080/api/counter")  // skrng local host dlu
            .then((res) => res.json())
            .then((data) => setViews(data))
            .catch(() => {
            });
    }, []);

    return (
        <main className="flex min-h-screen flex-col items-center justify-center gap-12 bg-slate-950 px-6 text-center text-slate-100">
            <div>
                <p className="text-sm uppercase tracking-[0.5em] text-emerald-400">
                    BidMart internal metrics
                </p>
                <h1 className="mt-4 text-6xl font-bold text-white">
                    Views: <span className="text-emerald-300">{views}</span>
                </h1>
            </div>

            <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-10 shadow-lg">
                <p className="text-lg text-slate-300">
                    wallet logic demo
                </p>
                <Link
                    href="/wallet"
                    className="mt-6 inline-flex items-center justify-center rounded-lg bg-emerald-500 px-6 py-3 text-lg font-semibold text-slate-950 transition hover:bg-emerald-400"
                >
                    Open Wallet Mockup
                </Link>
            </div>
        </main>
    );
}
