# Moinak Dey's Group:
1. Moinak Dey
2. Manikumar Honnenahalli Lakshminarayana Swamy
3. Nischith Bairannanavara Omprakash

# P2P-FileSharing:
In this Java-based project, we have developed a peer-to-peer (P2P) file-sharing software inspired by the BitTorrent protocol, a widely used P2P protocol for efficient file distribution. Notably, our implementation incorporates key features of BitTorrent, with a focus on the choking-unchoking mechanism.

## Steps to run the program:
Before executing the project, the following prerequisite must be met:
- Access the remote machine, transfer the source code, and compile it using the "make" command.
- To initiate the PeerProcesses on remote machines from the local machine, we employ the StartRemotePeers class. Executing this class will launch all the processes on the remote machines. However, it's important to note that this file requires the presence of the PeerInfo.cfg file.
- In order to establish connections with the remote machines from our local machines, the initial step involves generating SSH keys.

To generate the keys, follow the steps outlined below:
1. Execute the command "ssh-keygen"; it will prompt you for a file name and passphrase—please proceed without setting a passphrase.
2. The command "ssh-copy-id -i {your_generated_key_location} {username>@<remote_machine_url}" will copy the generated SSH public key to the ~/.ssh/authorized_keys file on the specified remote machine. This enables passwordless authentication for secure SSH connections from the local machine to the remote machine.
3. Transfer the Common.config file, PeerInfo.config file, and the target file for transfer to their respective directories within the "P2P/" folder in each peer's directory.
4. In the StartRemotePeers wrapper file, kindly update the following configurations: replace the 'username' with your username, set 'projPath' to the location where you run the program or PeerProcess exists, and use location of the generated 'id_rsa' for the 'pubKey' configuration..
5. To initiate the wrapper, execute the command "java StartRemotePeers".

#### [Demo Link](https://uflorida-my.sharepoint.com/personal/nischith_bairann_ufl_edu/_layouts/15/stream.aspx?id=%2Fpersonal%2Fnischith%5Fbairann%5Fufl%5Fedu%2FDocuments%2FFinalDemo%2Emp4&ga=1&referrer=StreamWebApp%2EWeb&referrerScenario=AddressBarCopied%2Eview)
