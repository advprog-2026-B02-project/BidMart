"use client";
import {useEffect, useState} from "react";

export default function Home() {
    const [views, setViews] = useState(0);

    useEffect(() => {
        fetch("http://localhost:8080/api/counter") // skrng local host dlu
            .then((res) => res.json())
            .then((data) => setViews(data))
            .catch(() => {
            });
    }, []);

    return (
        <main className="flex min-h-screen items-center justify-center bg-black">
            <h1 className="text-9xl font-bold text-white font-mono">
                Views : {views}
            </h1>
        </main>
    );
}