## mcscan
A simple command line port scanner to aid in penetration testing Minecraft servers

### Usage
```
mcscan [OPTIONS]

Options:
  -t, --target TEXT   Target IP
  -p, --port INT      Scan a single port
  -r, --range INT...  Scan a range
  -v, --verbose       Verbose output
  -h, --help          Show this message and exit
```

### What does it do?
- Scans a host for Minecraft servers
- Shows server query data (MOTD, player count, server version)
- Attempts to identify authentication mode (online/offline/IP forwarding). 
Not always successful due to whitelist, Forge, etc.

### Example output
```
Scanning target localhost, ports 25565-25700
Scanning [##########]: 100.0%  
 Port |                             MOTD | Players |                       Version |          Auth
------|----------------------------------|---------|-------------------------------|--------------
25565 |               A Minecraft Server |       0 |            Paper 1.16.5 (754) | IP forwarding
25566 | Test server - paper17-1 (online) |       0 |            Paper 1.17.1 (756) |        Online
25568 |               A Minecraft Server |       0 |             Spigot 1.8.8 (47) |        Online
25577 |            Another Bungee server |       0 | BungeeCord 1.8.x-1.17.x (755) |       Offline
```

### Why?
I mostly wrote this for fun, to practice both working with the Minecraft protocol and Kotlin. 

### On legality
Port scanning is a complicated, blurry, messy legal topic that I'm not even going to touch.
Nmap has [an article](https://nmap.org/book/legal-issues.html) that sums it up very nicely