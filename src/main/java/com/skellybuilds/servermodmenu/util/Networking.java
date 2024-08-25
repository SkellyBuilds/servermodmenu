package com.skellybuilds.servermodmenu.util;
import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.util.*;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.HostAndPort;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.logging.LogUtils;
import com.skellybuilds.servermodmenu.ModMenu;
import com.skellybuilds.servermodmenu.db.ModAdapter;
import com.skellybuilds.servermodmenu.db.SMod;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.*;
import net.minecraft.client.option.ServerList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;

import static com.skellybuilds.servermodmenu.ModMenu.MainNetwork;

public class Networking {
	public static final Logger LOGGER = LoggerFactory.getLogger("Server Mod Menu");
	private final Map<String, Socket> sockets = new HashMap<>(); // Ip address, Socket
	private final Map<String, Integer> ports = new HashMap<>();
	public Map<String, Thread> networkThreads = new HashMap<>();
	public Map<String, Thread> downloadThreads = new HashMap<>();
	public Map<String, String> networkErrors = new HashMap<>();
	private boolean logSER = true;

	public void connect(String ipaddress, int port){
		try {
			ports.put(ipaddress, port); // may be useful for debugging
			sockets.put(ipaddress, new Socket(ipaddress, port));
			logSER = true;
			networkErrors.put(ipaddress, "OK");
		//	LOGGER.info("Connected to socket!");
		} catch (Exception e) {
			if(!Objects.equals(networkErrors.get(ipaddress), "ERR")){
				LOGGER.error("Could not connect to the socket. Server may be down or SCMC is dead");
				networkErrors.put(ipaddress, "ERR");
			}
		}
	}

	public boolean isSocketValid(String ip){
		if(sockets.get(ip) == null) return false;
		if(sockets.get(ip).isClosed()){
			sockets.remove(ip);
			return false;
		} else {
			return true;
		}
	}

	public void sendDataToServer(String ip,String data) {
		if(sockets.get(ip) == null) return;
		// move this threads, thread map for ip?
		try (
			Socket socket = sockets.get(ip);
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())))
			{
			out.println(data);


			String response = in.readLine();

		//	LOGGER.info(response);

		} catch (Exception e) {
				LOGGER.error("Exception occurred while sending the data:");
				LOGGER.error(e.toString());
			}
	}

	public String requestNResponse(String ip,String data){
		if(sockets.get(ip) == null) return "DEADSOCKET";
		// move this threads, thread map for ip?
		try (
			Socket socket = sockets.get(ip);
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())))
		{
			out.println(data);

			return in.readLine();

		} catch (Exception e) {
			LOGGER.error("Exception occurred while sending the data:");
			LOGGER.error(e.toString());
			return "EXCEPTION";
		}

	}

	public boolean isNthreadsDone() {
		List<Boolean> listoft = new ArrayList<>();

		if(MainNetwork.networkThreads == null) return true;

		MainNetwork.networkThreads.forEach((id, thread) -> {
			if (thread.getState() != Thread.State.RUNNABLE) {
				listoft.add(true);
			}
		});

		return listoft.size() == networkThreads.size();
	}

	public boolean isDthreadsDone() {
		List<Boolean> listoft = new ArrayList<>();

		if(MainNetwork.downloadThreads == null) return true;

		MainNetwork.downloadThreads.forEach((id, thread) -> {
			if (thread.getState() != Thread.State.RUNNABLE) {
				listoft.add(true);
			}
		});

		return listoft.size() == MainNetwork.networkThreads.size();
	}

	public boolean isDthreadDone(String ip) {
		if(MainNetwork.downloadThreads.get(ip) == null) return true;
		return MainNetwork.downloadThreads.get(ip).getState() != Thread.State.RUNNABLE;
	}

	public void requestNDownload(String ip, String id){

		for (String s : ModMenu.idsDLD) {
			if(Objects.equals(s, id)){
				LOGGER.info("Mod {} already present", id);
				return;
			}
		}

		if(isModAlreadyPresent(id)){
			LOGGER.info("Mod {} already present", id);
			return;
		}

			do {
				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {
					LOGGER.error("Interrupted: {}", e.getMessage());
				}
			} while (!isDthreadDone(ip));

		Thread fD = new Thread(() -> {
			try {
				//PrintWriter out = new PrintWriter(sockets.get(ip).getOutputStream(), true);;
//				if(sockets.get(ip) == null || sockets.get(ip).isClosed()){

//				}

				connect(ip, ports.get(ip));
				while(!isSocketValid(ip)){
					if(Objects.equals(networkErrors.get(ip), "ERR")){
						LOGGER.info("Connection Failed! Retrying one more time");
						connect(ip, ports.get(ip));
						while(!isSocketValid(ip)){
							if(Objects.equals(networkErrors.get(ip), "ERR")){
								LOGGER.error("FAILED to connect!");
								return;
							}
						}
					} else {
						LOGGER.info("Waiting for connection {}", ip);
					}
				}




				String fileN = requestNResponse(ip, "getmod|" + id);

				if (fileN == null) {
					LOGGER.error("Could finds not mod's filename! Mod does not exist as a file!");
					return;
				}

				if(fileN.contains("fabric-api")){
				return;
				}

				Path modsFolderPath = Paths.get("./mods");
				if (!Files.exists(modsFolderPath)) {
					try {
						Files.createDirectories(modsFolderPath);
					} catch (IOException e) {
						LOGGER.error(e.toString());
					}
				}



				//Path filePath = modsFolderPath.resolve(fileN);
				try {

//					connect(ip, 27752);
//					a = isSocketValid(ip);
//					if (!a) {
//						return;
//					}
//
//					connect(ip, 27752);

					File modsDir = new File("./mods");
					if (!modsDir.exists()) {
						modsDir.mkdirs();
					}

					File modFile = new File(modsDir, fileN);
					try (BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(modFile))) {
						byte[] buffer = new byte[4096];
						int bytesRead;

//
						connect(ip, ports.get(ip));
						while(!isSocketValid(ip)){
							LOGGER.info("Waiting for connection {}", ip);
						}

						PrintWriter out = null;
						try {
							out = new PrintWriter(sockets.get(ip).getOutputStream(), true);
						} catch (IOException e) {
							LOGGER.error(e.toString());
						}
						BufferedInputStream in = null;
						try {
							in = new BufferedInputStream(sockets.get(ip).getInputStream());
						} catch (IOException e) {
							LOGGER.error(e.toString());
						}

						out.println("download|" + fileN);

						// Step 5: Read the file from the server and write it to the mods folder
						while ((bytesRead = in.read(buffer)) != -1) {
							fileOut.write(buffer, 0, bytesRead);
						}

						fileOut.close();

						Thread depT = new Thread(() -> {
							checkAndDownloadDependencies(ip, modFile.toPath());
						});
						depT.setName("Download Manager Dependency -" + depT.getId());
						depT.start();
						while (true) {
							if (depT.getState() != Thread.State.RUNNABLE) {
								break;
							}
						}

						LOGGER.info("Mod downloaded successfully: " + fileN);

						ModMenu.idsDLD.add(id);
					} catch (IOException e) {
						LOGGER.error("Error downloading mod: " + e.getMessage());
						LOGGER.error(e.toString());
						modFile.delete();
					}
				} catch (Exception e) {
					LOGGER.error(e.toString());
				}

			} finally {

			}
		});

		fD.setName("[ServerModMenu] Download Manager - "+ip+" - "+fD.getId());
		MainNetwork.downloadThreads.put(ip, fD);
		if(MainNetwork.downloadThreads.get(ip) != null){
			MainNetwork.downloadThreads.get(ip).start();
		}

	}

	public static boolean isModAlreadyPresent(String modName) {
		Optional<ModContainer> modContainerOptional = FabricLoader.getInstance().getModContainer(modName);
		if (modContainerOptional.isEmpty()) {
			//System.out.println("Mod not found: " + modName);
			Path modPath = Paths.get("./mods", modName + ".jar");
			return Files.exists(modPath);
		} else {
			return true;
		}


	}

	private void checkAndDownloadDependencies(String ip, Path modFilePath) {

		Thread td = new Thread(() ->  {
			try (JarFile jarFile = new JarFile(modFilePath.toFile())) {
				ZipEntry entry = jarFile.getEntry("fabric.mod.json");
				if (entry != null) {
					try (InputStream inputStream = jarFile.getInputStream(entry)) {
						JsonObject jsonObject = JsonParser.parseReader(new InputStreamReader(inputStream)).getAsJsonObject();
						if (jsonObject.has("depends")) {
							JsonObject dependencies = jsonObject.getAsJsonObject("depends");

							for (String dep : dependencies.keySet()) {
								String depVersion = dependencies.get(dep).getAsString();
								LOGGER.info("Dependency found: " + dep + " version: " + depVersion);
								// Implement logic to download the dependency mod here
								if (!isModAlreadyPresent(dep) && !Objects.equals(dep, "fabricloader")) {
									requestNDownload(ip, dep); // Recursive call to download dependency
								}
							}
						}
					}
				}
			} catch (IOException e) {
				LOGGER.error(e.toString());
				LOGGER.error("Failed to read mod dependencies.");
			}
		});

		td.start();
		while(true) {
			if(td.getState() != Thread.State.RUNNABLE){
				break;
			}
		}

		return;

	}

	public void reloadServer(String ip){
		final SMod[][] ModsA = {{}};



		if(ip.contains(":")){
			ip = ip.substring(0, ip.indexOf(":"));
		}
		int port;

		ServerAddress parsedAd = ServerAddress.parse(ip);

		Optional<InetSocketAddress> optAddress = AllowedAddressResolver.DEFAULT.resolve(parsedAd).map(Address::getInetSocketAddress);
		if(optAddress.isPresent()) {
			final InetSocketAddress inetSocketAddress = (InetSocketAddress) optAddress.get();
			//ip = inetSocketAddress.getHostName();
			port = inetSocketAddress.getPort();
		} else {
			port = 27752;
		}

		if(MainNetwork.networkThreads.get(ip) == null) {
			LOGGER.info("Creating Network Thread - "+ip);
			String finalIp = ip;
			Thread fD = new Thread(() -> {
				if (!MainNetwork.isSocketValid(finalIp)) {
					MainNetwork.connect(finalIp, port);
					boolean a = MainNetwork.isSocketValid(finalIp);
					if (a) {
						GsonBuilder gsonBuilder = new GsonBuilder();
						gsonBuilder.registerTypeAdapter(SMod.class, new ModAdapter());
						Gson gson = gsonBuilder.create();
						String str = MainNetwork.requestNResponse(finalIp, "getall");
						ModsA[0] = gson.fromJson(str, SMod[].class);
						LOGGER.info(Arrays.toString(ModsA[0]));
						boolean sinit = false;
						if(ModsA[0].length < 1){
							ModMenu.SMODS.computeIfAbsent(finalIp, k -> new HashMap<>());
						}
						for (SMod smod : ModsA[0]) {
							if(!sinit){
								ModMenu.SMODS.computeIfAbsent(finalIp, k -> new HashMap<>());
								ModMenu.SMODSA = new HashMap<>();
								sinit = true;
							}

							smod.server = finalIp;
							ModMenu.SMODS.get(finalIp).put(smod.getId(), smod);


						}
					} else ModMenu.SMODS.computeIfAbsent(finalIp, k -> new HashMap<>());

					;
				} else if (!MainNetwork.isSocketValid(finalIp)) {
					if (ModsA[0].length < 1) {
						ModMenu.SMODS.computeIfAbsent(finalIp, k -> new HashMap<>());
					}
					LOGGER.error("Unable to connect to" + finalIp);
				}
				else {
					boolean a = MainNetwork.isSocketValid(finalIp);
					if(a) {
						GsonBuilder gsonBuilder = new GsonBuilder();
						gsonBuilder.registerTypeAdapter(SMod.class, new ModAdapter());
						Gson gson = gsonBuilder.create();
						String str = MainNetwork.requestNResponse(finalIp, "getall");
						ModsA[0] = gson.fromJson(str, SMod[].class);
						LOGGER.info(Arrays.toString(ModsA[0]));
						boolean sinit = false;
						if (ModsA[0].length < 1) {
							ModMenu.SMODS.computeIfAbsent(finalIp, k -> new HashMap<>());
						}
						for (SMod smod : ModsA[0]) {
							if (!sinit) {
								ModMenu.SMODS.computeIfAbsent(finalIp, k -> new HashMap<>());
								ModMenu.SMODSA = new HashMap<>();
								sinit = true;
							}

							smod.server = finalIp;
							ModMenu.SMODS.get(finalIp).put(smod.getId(), smod);
						}
					}
				}
			});
			fD.setName("[ServerModMenu] Main network - " + ip+ " - "+ fD.getId());
			MainNetwork.networkThreads.put(ip, fD);
			MainNetwork.networkThreads.get(ip).start();

		}else {
			if(MainNetwork.networkThreads.get(ip).getState() != Thread.State.RUNNABLE) {
				MainNetwork.networkThreads.remove(ip);
				LOGGER.info("Creating Network Thread - "+ip);
				String finalIp1 = ip;
				Thread fD = new Thread(() -> {
					if (!MainNetwork.isSocketValid(finalIp1)) {
						MainNetwork.connect(finalIp1, port);
						boolean a = MainNetwork.isSocketValid(finalIp1);
						if (a) {
							GsonBuilder gsonBuilder = new GsonBuilder();
							gsonBuilder.registerTypeAdapter(SMod.class, new ModAdapter());
							Gson gson = gsonBuilder.create();
							String str = MainNetwork.requestNResponse(finalIp1, "getall");
							ModsA[0] = gson.fromJson(str, SMod[].class);
							LOGGER.info(Arrays.toString(ModsA[0]));
							boolean sinit = false;
							if(ModsA[0].length < 1){
								ModMenu.SMODS.computeIfAbsent(finalIp1, k -> new HashMap<>());
							}
							for (SMod smod : ModsA[0]) {
								if(!sinit){
									ModMenu.SMODS.computeIfAbsent(finalIp1, k -> new HashMap<>());
									ModMenu.SMODSA = new HashMap<>();
									sinit = true;
								}
								smod.server = finalIp1;
								ModMenu.SMODS.get(finalIp1).put(smod.getId(), smod);

							}
						} else ModMenu.SMODS.computeIfAbsent(finalIp1, k -> new HashMap<>());
						;
					} else if (!MainNetwork.isSocketValid(finalIp1))
						LOGGER.error("Unable to connect to" + finalIp1);
					else {
						boolean a = MainNetwork.isSocketValid(finalIp1);
						if(a) {
							GsonBuilder gsonBuilder = new GsonBuilder();
							gsonBuilder.registerTypeAdapter(SMod.class, new ModAdapter());
							Gson gson = gsonBuilder.create();
							String str = MainNetwork.requestNResponse(finalIp1, "getall");
							ModsA[0] = gson.fromJson(str, SMod[].class);
							LOGGER.info(Arrays.toString(ModsA[0]));
							boolean sinit = false;
							if (ModsA[0].length < 1) {
								ModMenu.SMODS.computeIfAbsent(finalIp1, k -> new HashMap<>());
							}
							for (SMod smod : ModsA[0]) {
								if (!sinit) {
									ModMenu.SMODS.computeIfAbsent(finalIp1, k -> new HashMap<>());
									ModMenu.SMODSA = new HashMap<>();
									sinit = true;
								}

								smod.server = finalIp1;
								ModMenu.SMODS.get(finalIp1).put(smod.getId(), smod);

							}
						}
					}
				});
				fD.setName("[ServerModMenu] Main network - " + ip+ " - "+ fD.getId());
				MainNetwork.networkThreads.put(ip, fD);
				MainNetwork.networkThreads.get(ip).start();
			}
			else LOGGER.info("Waiting for thread - {} to finish!", MainNetwork.networkThreads.get(ip).getName());
		}
	}


	public void reloadAllServers(ServerList serverList){

		serverList.loadFile();
		final SMod[][] ModsA = {{}};


		for (int i = 0; i < serverList.size(); i++) {
			ServerInfo serverInfo = serverList.get(i);
			int port;

			if(serverInfo.address.contains(":")){
				serverInfo.address = serverInfo.address.substring(0, serverInfo.address.indexOf(":"));
			} // a port found

			Networking.ServerAddress parsedAd = Networking.ServerAddress.parse(serverInfo.address);

			Optional<InetSocketAddress> optAddress = Networking.AllowedAddressResolver.DEFAULT.resolve(parsedAd).map(Address::getInetSocketAddress);
			if(optAddress.isPresent()){
				final InetSocketAddress inetSocketAddress = (InetSocketAddress) optAddress.get();

				//serverInfo.address = inetSocketAddress.getAddress().getHostAddress();
				port = inetSocketAddress.getPort();
			} else {
				port = 27752;
			}

			if(MainNetwork.networkThreads.get(serverInfo.address) == null) {
				LOGGER.info("Creating Network Thread - "+serverInfo.address);
				Thread fD = new Thread(() -> {
					if (!MainNetwork.isSocketValid(serverInfo.address)) {
						MainNetwork.connect(serverInfo.address, port);
						boolean a = MainNetwork.isSocketValid(serverInfo.address);
						if (a) {
							GsonBuilder gsonBuilder = new GsonBuilder();
							gsonBuilder.registerTypeAdapter(SMod.class, new ModAdapter());
							Gson gson = gsonBuilder.create();
							MainNetwork.connect(serverInfo.address, port);
							String str = MainNetwork.requestNResponse(serverInfo.address, "getall");
							ModsA[0] = gson.fromJson(str, SMod[].class);
							LOGGER.info(Arrays.toString(ModsA[0]));
							boolean sinit = false;
							if(ModsA[0].length < 1){
								ModMenu.SMODS.computeIfAbsent(serverInfo.address, k -> new HashMap<>());
							}
							for (SMod smod : ModsA[0]) {
								if(!sinit){
									ModMenu.SMODS.computeIfAbsent(serverInfo.address, k -> new HashMap<>());
									ModMenu.SMODSA = new HashMap<>();
									sinit = true;
								}

								smod.server = serverInfo.address;
								ModMenu.SMODS.get(serverInfo.address).put(smod.getId(), smod);


							}
						} else ModMenu.SMODS.computeIfAbsent(serverInfo.address, k -> new HashMap<>());

						;
					} else if (!MainNetwork.isSocketValid(serverInfo.address)) {
						if (ModsA[0].length < 1) {
							ModMenu.SMODS.computeIfAbsent(serverInfo.address, k -> new HashMap<>());
						}
						LOGGER.error("Unable to connect to" + serverInfo.address);
					}
					else {
						boolean a = MainNetwork.isSocketValid(serverInfo.address);
						if(a) {
							GsonBuilder gsonBuilder = new GsonBuilder();
							gsonBuilder.registerTypeAdapter(SMod.class, new ModAdapter());
							Gson gson = gsonBuilder.create();
							MainNetwork.connect(serverInfo.address, port);
							String str = MainNetwork.requestNResponse(serverInfo.address, "getall");
							ModsA[0] = gson.fromJson(str, SMod[].class);
							LOGGER.info(Arrays.toString(ModsA[0]));
							boolean sinit = false;
							if (ModsA[0].length < 1) {
								ModMenu.SMODS.computeIfAbsent(serverInfo.address, k -> new HashMap<>());
							}
							for (SMod smod : ModsA[0]) {
								if (!sinit) {
									ModMenu.SMODS.computeIfAbsent(serverInfo.address, k -> new HashMap<>());
									ModMenu.SMODSA = new HashMap<>();
									sinit = true;
								}

								smod.server = serverInfo.address;
								ModMenu.SMODS.get(serverInfo.address).put(smod.getId(), smod);
							}
						}
					}
				});
				fD.setName("[ServerModMenu] Main network - " + serverInfo.address+ " - "+ fD.getId());
				MainNetwork.networkThreads.put(serverInfo.address, fD);
				MainNetwork.networkThreads.get(serverInfo.address).start();

			} else {
				if(MainNetwork.networkThreads.get(serverInfo.address).getState() != Thread.State.RUNNABLE) {
					MainNetwork.networkThreads.remove(serverInfo.address);
					LOGGER.info("Creating Network Thread - "+serverInfo.address);
					Thread fD = new Thread(() -> {
						if (!MainNetwork.isSocketValid(serverInfo.address)) {
							MainNetwork.connect(serverInfo.address, port);
							boolean a = MainNetwork.isSocketValid(serverInfo.address);
							if (a) {
								GsonBuilder gsonBuilder = new GsonBuilder();
								gsonBuilder.registerTypeAdapter(SMod.class, new ModAdapter());
								Gson gson = gsonBuilder.create();
								MainNetwork.connect(serverInfo.address, port);
								String str = MainNetwork.requestNResponse(serverInfo.address, "getall");
								ModsA[0] = gson.fromJson(str, SMod[].class);
								LOGGER.info(Arrays.toString(ModsA[0]));
								boolean sinit = false;
								if(ModsA[0].length < 1){
									ModMenu.SMODS.computeIfAbsent(serverInfo.address, k -> new HashMap<>());
								}
								for (SMod smod : ModsA[0]) {
									if(!sinit){
										ModMenu.SMODS.computeIfAbsent(serverInfo.address, k -> new HashMap<>());
										ModMenu.SMODSA = new HashMap<>();
										sinit = true;
									}

									smod.server = serverInfo.address;
									ModMenu.SMODS.get(serverInfo.address).put(smod.getId(), smod);

								}
							} else ModMenu.SMODS.computeIfAbsent(serverInfo.address, k -> new HashMap<>());
							;
						} else if (!MainNetwork.isSocketValid(serverInfo.address))
							LOGGER.error("Unable to connect to" + serverInfo.address);
						else {
							boolean a = MainNetwork.isSocketValid(serverInfo.address);
							if(a) {
								GsonBuilder gsonBuilder = new GsonBuilder();
								gsonBuilder.registerTypeAdapter(SMod.class, new ModAdapter());
								Gson gson = gsonBuilder.create();
								MainNetwork.connect(serverInfo.address, port);
								String str = MainNetwork.requestNResponse(serverInfo.address, "getall");
								ModsA[0] = gson.fromJson(str, SMod[].class);
								LOGGER.info(Arrays.toString(ModsA[0]));
								boolean sinit = false;
								if (ModsA[0].length < 1) {
									ModMenu.SMODS.computeIfAbsent(serverInfo.address, k -> new HashMap<>());
								}
								for (SMod smod : ModsA[0]) {
									if (!sinit) {
										ModMenu.SMODS.computeIfAbsent(serverInfo.address, k -> new HashMap<>());
										ModMenu.SMODSA = new HashMap<>();
										sinit = true;
									}

									smod.server = serverInfo.address;
									ModMenu.SMODS.get(serverInfo.address).put(smod.getId(), smod);

								}
							}
						}
					});
					fD.setName("[ServerModMenu] Main network - " + serverInfo.address+ " - "+ fD.getId());
					MainNetwork.networkThreads.put(serverInfo.address, fD);
					MainNetwork.networkThreads.get(serverInfo.address).start();
				}
				else LOGGER.info("Waiting for thread - {} to finish!", MainNetwork.networkThreads.get(serverInfo.address).getName());
			}
		}
	}

	public static class SocketStatusLoop implements Runnable {
		private final String ip;
		private int port = 27752;
		public int status; // 0 = OK 1 = CONNECTING/BUSY 2 = OFFLINE
		private boolean connected;
		private boolean wasD = true;

		public SocketStatusLoop(String ip){
			if(ip.contains(":")){
				ip = ip.substring(0, ip.indexOf(":"));
			}
			this.ip = ip;
			long startTime = System.currentTimeMillis();

			LOGGER.info("[ServerModMenu] Starting Socket Status Loop - {} - {}", Thread.currentThread().getId(), startTime);
		}

		public SocketStatusLoop(String ip, int Port){
			if(ip.contains(":")){
				ip = ip.substring(0, ip.indexOf(":"));
			}
			this.ip = ip;
			this.port = Port;
			long startTime = System.currentTimeMillis();

			LOGGER.info("[ServerModMenu] Starting Socket Status Loop - {} - {}", Thread.currentThread().getId(), startTime);
		}

		@Override
		public void run() {
			while (true) {
				try {
					status = 1;
					MainNetwork.connect(ip, port);
					if(!MainNetwork.isSocketValid(ip)){
						status = 2;
						wasD = true;
						Thread.sleep(8000);
					} else {
						String res = MainNetwork.requestNResponse(ip, "hello");
						if (!Objects.equals(res, "ok")) {
							wasD = true;
							status = 2;
						} else {
							if(wasD){
								MinecraftClient client = MinecraftClient.getInstance();
								ServerList serverList = new ServerList(client);
								ModMenu.sendmodstonetwork(serverList, client);
							}
							status = 0;
							wasD = false;
						}
						if (status == 2) Thread.sleep(8500);
						else Thread.sleep(4550);
					}
				} catch (InterruptedException e) {
					LOGGER.error("Socket Status Loop interrupted: {}", e.getMessage());
					Thread.currentThread().interrupt(); // Restore the interrupted status
					break; // Exit the loop if interrupted
				}
			}
		}
	}

	static int portOrDefault(String port) {
		try {
			return Integer.parseInt(port.trim());
		} catch (Exception var2) {
			return 27752;
		}
	}


	public static class AllowedAddressResolver {
		public static final Networking.AllowedAddressResolver DEFAULT;
		private final Networking.AddressResolver addressResolver;
		private final Networking.RedirectResolver redirectResolver;
		//private final BlockListChecker blockListChecker;

		@VisibleForTesting
		AllowedAddressResolver(Networking.AddressResolver addressResolver, Networking.RedirectResolver redirectResolver) {
			this.addressResolver = addressResolver;
			this.redirectResolver = redirectResolver;
		}

		public Optional<Address> resolve(ServerAddress address) {
			Optional<Address> optional = this.addressResolver.resolve(address);
			if (optional.isPresent()){
				Optional<ServerAddress> optional2 = this.redirectResolver.lookupRedirect(address);
				if (optional2.isPresent()) {
					optional = this.addressResolver.resolve((ServerAddress)optional2.get());
				}

				return optional;
			} else {
				return Optional.empty();
			}
		}

		static {
			DEFAULT = new Networking.AllowedAddressResolver(Networking.AddressResolver.DEFAULT, Networking.RedirectResolver.createSrv());
		}
	}


	public interface AddressResolver {
		Logger LOGGER = LogUtils.getLogger();
		Networking.AddressResolver DEFAULT = (address) -> {
			try {
				InetAddress inetAddress = InetAddress.getByName(address.getAddress());
				return Optional.of(Address.create(new InetSocketAddress(inetAddress, address.getPort())));
			} catch (UnknownHostException var2) {
				UnknownHostException unknownHostException = var2;
				LOGGER.debug("Couldn't resolve server {} address", address.getAddress(), unknownHostException);
				return Optional.empty();
			}
		};

		Optional<Address> resolve(ServerAddress address);
	}


	public interface RedirectResolver {
		Logger LOGGER = LogUtils.getLogger();
		Networking.RedirectResolver INVALID = (address) -> {
			return Optional.empty();
		};

		Optional<ServerAddress> lookupRedirect(ServerAddress address);

		static Networking.RedirectResolver createSrv() {
			InitialDirContext dirContext;
			try {
				String string = "com.sun.jndi.dns.DnsContextFactory";
				Class.forName("com.sun.jndi.dns.DnsContextFactory");
				Hashtable<String, String> hashtable = new Hashtable<>(); // Why mojang?
				hashtable.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
				hashtable.put("java.naming.provider.url", "dns:");
				hashtable.put("com.sun.jndi.dns.timeout.retries", "1");
				dirContext = new InitialDirContext(hashtable);
			} catch (Throwable var3) {
				Throwable throwable = var3;
				LOGGER.error("Failed to initialize SRV redirect resolved, some servers might not work", throwable);
				return INVALID;
			}

			return (address) -> {
				if (address.getPort() == 27752) {
					try {
						Attributes attributes = dirContext.getAttributes("_scmc._tcp." + address.getAddress(), new String[]{"SRV"});
						Attribute attribute = attributes.get("srv");
						if (attribute != null) {
							String[] strings = attribute.get().toString().split(" ", 4);
							return Optional.of(new ServerAddress(strings[3], Networking.portOrDefault(strings[2])));
						}
					} catch (Throwable var5) {
					}
				}

				return Optional.empty();
			};
		}
	}

	public static final class ServerAddress {
		private static final Logger LOGGER = LogUtils.getLogger();
		private final HostAndPort hostAndPort;
		private static final Networking.ServerAddress INVALID = new Networking.ServerAddress(HostAndPort.fromParts("server.invalid", 25565));

		public ServerAddress(String host, int port) {
			this(HostAndPort.fromParts(host, port));
		}

		private ServerAddress(HostAndPort hostAndPort) {
			this.hostAndPort = hostAndPort;
		}

		public String getAddress() {
			try {
				return IDN.toASCII(this.hostAndPort.getHost());
			} catch (IllegalArgumentException var2) {
				return "";
			}
		}

		public int getPort() {
			return this.hostAndPort.getPort();
		}

		public static Networking.ServerAddress parse(String address) {
			if (address == null) {
				return INVALID;
			} else {
				try {
					HostAndPort hostAndPort = HostAndPort.fromString(address).withDefaultPort(27752);
					return hostAndPort.getHost().isEmpty() ? INVALID : new Networking.ServerAddress(hostAndPort);
				} catch (IllegalArgumentException var2) {
					IllegalArgumentException illegalArgumentException = var2;
					LOGGER.info("Failed to parse URL {}", address, illegalArgumentException);
					return INVALID;
				}
			}
		}

		public static boolean isValid(String address) {
			try {
				HostAndPort hostAndPort = HostAndPort.fromString(address);
				String string = hostAndPort.getHost();
				if (!string.isEmpty()) {
					IDN.toASCII(string);
					return true;
				}
			} catch (IllegalArgumentException var3) {
			}

			return false;
		}

		static int portOrDefault(String port) {
			try {
				return Integer.parseInt(port.trim());
			} catch (Exception var2) {
				return 27752;
			}
		}

		public String toString() {
			return this.hostAndPort.toString();
		}

		public boolean equals(Object o) {
			if (this == o) {
				return true;
			} else {
				return o instanceof ServerAddress && this.hostAndPort.equals(((ServerAddress) o).hostAndPort);
			}
		}

		public int hashCode() {
			return this.hostAndPort.hashCode();
		}
	}


}
