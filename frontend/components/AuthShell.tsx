"use client";

import Image from "next/image";

export default function AuthShell({
                                      title,
                                      subtitle,
                                      children,
                                  }: {
    title: string;
    subtitle: string;
    children: React.ReactNode;
}) {
    return (
        <div className="min-h-screen bg-[#F5F1E8] grid grid-cols-1 lg:grid-cols-2">
            <div className="hidden lg:flex bg-[#002447] items-center justify-center p-12">
                <div className="text-center text-white">
                    <div className="relative mx-auto mb-1 h-40 w-64">
                        <Image
                            src="/bidmart-logo.png"
                            alt="BidMart"
                            fill
                            className="object-contain"
                            priority
                        />
                    </div>

                    <div className="text-6xl font-semibold tracking-wide text-white">BidMart</div>

                    <div className="mx-auto my-6 h-px w-80 bg-white/50"/>

                    <div className="text-2xl font-light italic text-white">
                        “Bid kilat, hasil tetap akurat“
                    </div>
                </div>
            </div>

            <div className="flex items-center justify-center p-6 lg:p-12">
                <div
                    className="w-full max-w-2xl rounded-[28px] bg-white border border-black/5 shadow-sm px-10 py-10 text-black">
                    <div className="text-center">
                        <h1 className="text-4xl font-semibold text-[#002447]">{title}</h1>
                        <p className="mt-2 text-lg text-black/70">{subtitle}</p>
                    </div>

                    <div className="mt-10 text-black">
                        {children}
                    </div>
                </div>
            </div>
        </div>
    );
}