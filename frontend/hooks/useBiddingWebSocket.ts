// hooks/useBiddingWebSocket.ts

import {useEffect, useState, useRef, useCallback} from 'react';
import {Client} from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import {BidResponseDTO} from '@/lib/bidding.api';

interface WebSocketHookProps {
    auctionId: string;
}

interface WebSocketPayload {
    type: 'NEW_BID' | 'AUCTION_EXTENDED' | 'AUCTION_ENDED' | string;
    data: {
        // field untuk NEW_BID
        bidderName?: string;
        amount?: number;
        minimumNextBid?: number;
        bidCount?: number;
        timestamp?: string;

        // field untuk AUCTION_EXTENDED
        newEndTime?: string;
        extensionCount?: number;

        // field untuk AUCTION_ENDED
        status?: string;
        winningBid?: number;
        winnerName?: string;
    };
}

export function useBiddingWebSocket({auctionId}: WebSocketHookProps) {
    const [bids, setBids] = useState<BidResponseDTO[]>([]);
    const [auctionStatus, setAuctionStatus] = useState<string>('');
    const [isConnected, setIsConnected] = useState<boolean>(false);
    const clientRef = useRef<Client | null>(null);

    const handleWebSocketMessage = useCallback((payload: WebSocketPayload) => {
        switch (payload.type) {
            case 'NEW_BID':
                const newBid: BidResponseDTO = {
                    id: `ws-${Date.now()}`, // id sementara untuk list key di frontend
                    auctionId: auctionId,
                    bidderId: payload.data.bidderName || 'Unknown',
                    amount: payload.data.amount || 0,
                    status: 'ACCEPTED',
                    isNewHighBid: true,
                    createdAt: payload.data.timestamp || new Date().toISOString()
                };
                setBids((prevBids) => [newBid, ...prevBids]);
                break;

            case 'AUCTION_EXTENDED':
                setAuctionStatus('EXTENDED');
                break;

            case 'AUCTION_ENDED':
                setAuctionStatus(payload.data.status || 'CLOSED');
                break;

            default:
                console.warn('Unhandled message type:', payload.type);
        }
    }, [auctionId]);

    useEffect(() => {
        if (!auctionId) return;

        const token = localStorage.getItem('accessToken');
        const socket = new SockJS('http://localhost:8080/ws');

        const stompClient = new Client({
            webSocketFactory: () => socket,
            connectHeaders: {
                ...(token ? {Authorization: `Bearer ${token}`} : {})
            },
            onConnect: () => {
                setIsConnected(true);

                stompClient.subscribe(`/topic/auctions/${auctionId}`, (message) => {
                    if (message.body) {
                        // casting hasil JSON.parse ke interface yang udah dibuat
                        const payload = JSON.parse(message.body) as WebSocketPayload;
                        handleWebSocketMessage(payload);
                    }
                });
            },
            onStompError: (frame) => {
                console.error('Broker reported error: ' + frame.headers['message']);
                console.error('Additional details: ' + frame.body);
                setIsConnected(false);
            },
            onDisconnect: () => {
                setIsConnected(false);
            }
        });

        stompClient.activate();
        clientRef.current = stompClient;

        return () => {
            if (stompClient.active) {
                stompClient.deactivate();
            }
        };
    }, [auctionId, handleWebSocketMessage]);

    return {bids, auctionStatus, isConnected};
}