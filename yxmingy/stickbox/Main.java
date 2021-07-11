package yxmingy.stickbox;
import cn.nukkit.Player;
import cn.nukkit.block.Block;
import cn.nukkit.blockentity.BlockEntity;
import cn.nukkit.blockentity.BlockEntityChest;
import cn.nukkit.command.*;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.block.BlockBreakEvent;
import cn.nukkit.event.player.PlayerInteractEvent;
import cn.nukkit.item.Item;
import cn.nukkit.nbt.tag.CompoundTag;
import cn.nukkit.nbt.tag.StringTag;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import cn.nukkit.level.Position;
import cn.nukkit.utils.TextFormat;
import org.checkerframework.checker.units.qual.C;

import java.util.*;


public class Main extends PluginBase implements Listener {
	public Config conf;
	private Config boxs;
	private List lockp = new LinkedList();
	private List unlockp = new LinkedList();
	private List setkeyp = new LinkedList();
	private List upboxp = new LinkedList();
	/**
	 * [ {sticker:blockstr} => (left-click-time) ]
	 */
	private Map stickp = new LinkedHashMap<String,Integer>();
	public void onEnable() {
		getLogger().notice(TextFormat.BLUE+"StickBox works! Author: xMing.");
		getServer().getPluginManager().registerEvents(this,this);
		LinkedHashMap<String, Object> data = new LinkedHashMap<String, Object>(),
		                              boxsMap = new LinkedHashMap<String, Object>();
		data.put("2级所需金锭",32);
		data.put("3级所需金锭",64);
		conf = new Config(getDataFolder()+"/Config.yml",Config.YAML,data);
		boxs = new Config(getDataFolder()+"/Boxs.yml",Config.YAML,boxsMap);
		/*['world:x:y:z':
		 *   {
		 *     owner：Steve
		 *     level: 3
		 *   }
		 * ]
		 */
	}
	public void onDisable() {
		getLogger().warning("[YMenu] Bye bye~");
	}
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if(!(sender instanceof Player)) {
			sender.sendMessage("控制台nmsl");
			return true;
		}
		switch (command.getName()) {
			case "锁":
				lockp.add(sender.getName());
				sender.sendMessage("请点击你要锁的箱子");
				break;
			case "解":
				unlockp.add(sender.getName());
				sender.sendMessage("请点击你要解锁的箱子");
				break;
			case "设计钥匙":
				setkeyp.add(sender.getName());
				sender.sendMessage("请用骨头点击你要设置钥匙的箱子");
				break;
			case "升级箱子":
				upboxp.add(sender.getName());
				sender.sendMessage("请点击你要升级的箱子");
				break;
		}
		return true;
	}
	private String blockToString(Block bblock) {
		return bblock.getLevelName() + "," + bblock.getFloorX() + "," + bblock.getFloorY() + "," + bblock.getFloorZ();
	}
	public boolean isLocked(Block bblock) {
		return null != getActualBlockString(bblock);
	}
	public String getActualBlockString(Block bblock) {
		String block = blockToString(bblock);
		if(boxs.exists(block)) {
			return block;
		}else{
			//如果该箱子不被锁，查看隔壁箱子是否被锁
			BlockEntityChest be = (BlockEntityChest)bblock.getLevel().getBlockEntity(bblock);
			if (be instanceof BlockEntityChest) {
				if (be.isPaired()) {
					block = blockToString(be.getPair().getBlock());
					if(boxs.exists(block)) {
						return block;
					}
				}
			}
		}
		return null;
	}
	//把优先级拉满 避免受到世界保护插件的影响
	@EventHandler(priority = EventPriority.MONITOR)
	public void onBreak(BlockBreakEvent event) {
		if(event.getBlock().getId() != 54)
			return;
		String block = getActualBlockString(event.getBlock());
		if(null != block) {
			String owner = ((LinkedHashMap<String,String>)(boxs.get(block))).get("owner");
			if(owner.equals("Console") || owner.equals(event.getPlayer().getName())){
				boxs.remove(block);
				event.getPlayer().sendMessage("箱子已被破坏，原等级清空");
			}else {
				event.setCancelled();
				event.getPlayer().sendMessage("挖别人箱子，你瞧瞧你这是人干的事吗？");
			}
		}
	}
	@EventHandler(priority = EventPriority.MONITOR)
	public void onTouch(PlayerInteractEvent event) {
		if(event.getBlock().getId() != 54)
			return;
		Player player  = event.getPlayer();
		String name = event.getPlayer().getName();
		String world = event.getPlayer().getLevelName();
		Block bblock = event.getBlock();
		String block = getActualBlockString(bblock);
		boolean exist = null != block;
		String owner = "Console";

		if(exist) {
			owner = ((LinkedHashMap<String,String>)(boxs.get(block))).get("owner");
		}
		int item = event.getItem().getId();
		if(exist && !owner.equals("Console") && !owner.equals(name)) {
			if (player.getInventory().getItemInHand().getId() == 369) {
				if (((LinkedHashMap<String, String>) (boxs.get(block))).get("level").equals("3")) {
					player.sendMessage("没想到吧，这箱子是三级的，你撬不开，气死你");
					event.setCancelled();
					return;
				} else if(((LinkedHashMap<String, String>) (boxs.get(block))).get("level").equals("2")) {
					String stickstr = player.getName()+":"+block;
					if (!stickp.containsKey(stickstr)) {
						stickp.put(stickstr,499);
						player.sendMessage("二级箱子，共需要撬500下，还剩499下");
						event.setCancelled();
					}else {
						int left = (int)stickp.get(stickstr)-1;
						if(left > 0) {
							player.sendMessage("还剩" + left + "下就撬开了，加油");
							stickp.put(stickstr,left);
							event.setCancelled();
						}else {
							stickp.remove(stickstr);
							player.sendMessage("骐骥一跃，不能十步，驽马十驾，功在不舍。恭喜你撬开了这个箱子,撬棍作废");
							player.getInventory().setItemInHand(new Item(0,0,0));
						}
					}
					return;
				}else{
					player.sendMessage("没想到你真的会撬锁，小瞧你了,撬棍作废");
					player.getInventory().setItemInHand(new Item(0,0,0));
					return;
				}
			}else if(player.getInventory().getItemInHand().getLore().length>0 && player.getInventory().getItemInHand().getLore()[0].equals(owner+":"+block)) {
				player.sendMessage("你已用钥匙开锁");
				return;
			}else {
				player.sendMessage("箱子有锁，你会撬锁吗？");
				event.setCancelled();
				return;
			}
		}
		//箱子未上锁或是自己的
		if (lockp.contains(name)) {
			LinkedHashMap<String,String> box;
			if (exist) {
				box = (LinkedHashMap<String,String>)boxs.get(blockToString(bblock));
				box.put("owner",name);
			}else{
				box =  new LinkedHashMap<>();
				box.put("owner",name);
				box.put("level","1");
			}
			boxs.set(blockToString(bblock),box);
			boxs.save();
			player.sendMessage("箱子已上锁");
			event.setCancelled();
			lockp.remove(name);
		}
		if(unlockp.contains(name)) {
			if(exist && !owner.equals("Console")) {
				LinkedHashMap<String,String> box = (LinkedHashMap<String,String>)boxs.get(block);
				box.put("owner","Console");
				boxs.set(block,box);
				boxs.save();
				player.sendMessage("箱子已解锁，箱子等级保留");
			}else {
				player.sendMessage("箱子未上锁");
			}
			event.setCancelled();
			unlockp.remove(name);
		}
		if(setkeyp.contains(name)) {
			event.setCancelled();
			setkeyp.remove(name);
			if (exist && !owner.equals("Console")) {
				if(player.getInventory().getItemInHand().getId() == 352) {
					player.sendMessage("已设置钥匙");
					Item key = player.getInventory().getItemInHand();
//					** NBT have some fucking problems
//					CompoundTag nbt = new CompoundTag();
//					nbt.put("key",new StringTag(block));
//					key.setNamedTag(nbt);
					key.setLore(owner+":"+block);
					key.setCustomName(owner+"的箱子的钥匙");
					player.getInventory().setItemInHand(key);
				}else{
					player.sendMessage("要用骨头哦");
				}
			}else{
				player.sendMessage("箱子未上锁");
			}
		}
		if(upboxp.contains(name)) {
			if(exist) {
				int cost;
				LinkedHashMap<String,String> box = (LinkedHashMap<String,String>)boxs.get(block);
				switch (box.get("level")) {
					case "1":
						cost = conf.getInt("2级所需金锭");
						box.put("level","2");
						break;
					case "2":
						cost = conf.getInt("3级所需金锭");
						box.put("level","3");
						break;
					default:
						cost = 999999999;
				}
				Item itemm = new Item(266,0,cost);
				if(!player.getInventory().contains(itemm)) {
					player.sendMessage("需要金锭："+cost);
					player.sendMessage("你根本没有足够的物品，滚！");
				}else{
					player.getInventory().removeItem(itemm);
					boxs.set(block,box);
					boxs.save();
					player.sendMessage("升级成功，当前等级"+box.get("level"));

				}
			}
			event.setCancelled();
			upboxp.remove(name);
		}
	}
}
