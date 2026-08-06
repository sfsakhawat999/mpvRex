## Problem

SMB streaming through the local HTTP proxy caps out at ~4-6 MB/s on Wi-Fi
regardless of available bandwidth. NanoHTTPD 2.3.1 streams response bodies
by reading the wrapped InputStream in hard-coded 16 KiB chunks
(`Response.sendBody`), and each chunk became one synchronous SMB2 READ
round-trip. Throughput = 16 KiB / RTT (≈4-6 MB/s at 3-5 ms Wi-Fi RTT).

## Fix

New `PrefetchingSmbInputStream`: a daemon thread issues large (1 MiB)
SMB2 reads ahead of the consumer into a double-buffered block queue
(ArrayBlockingQueue, depth 2). NanoHTTPD keeps reading its 16 KiB chunks,
but they are served from memory instead of the network, making throughput
bandwidth-bound instead of latency-bound. The prefetch thread self-limits
to the response `contentLength` so an abandoned connection does not pull
the whole file. FTP/WebDAV streams get a 1 MiB BufferedInputStream as well.

## Measurements

- Simulation (3 ms RTT): 4.0 MB/s → 250 MB/s
- On device (Wi-Fi, 20 GB MKV over SMB): buffering speed 4-6 MB/s → ~20 MB/s
  (now limited by Wi-Fi bandwidth, comparable to VLC)

## Tested

- SMB/FTP/WebDAV playback, seeking, continuous rapid seeking, resume
