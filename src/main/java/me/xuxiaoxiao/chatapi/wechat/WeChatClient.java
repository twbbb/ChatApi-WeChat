package me.xuxiaoxiao.chatapi.wechat;

import me.xuxiaoxiao.chatapi.wechat.entity.contact.WXContact;
import me.xuxiaoxiao.chatapi.wechat.entity.contact.WXGroup;
import me.xuxiaoxiao.chatapi.wechat.entity.contact.WXUser;
import me.xuxiaoxiao.chatapi.wechat.entity.message.*;
import me.xuxiaoxiao.chatapi.wechat.protocol.*;
import me.xuxiaoxiao.xtools.common.XTools;
import me.xuxiaoxiao.xtools.common.http.XRequest;

import java.io.File;
import java.io.IOException;
import java.net.HttpCookie;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模拟网页微信客户端
 */
public final class WeChatClient
{
	public static final String LOGIN_TIMEOUT = "登陆超时";
	public static final String LOGIN_EXCEPTION = "登陆异常";
	public static final String INIT_EXCEPTION = "初始化异常";
	public static final String LISTEN_EXCEPTION = "监听异常";

	private static final Logger LOGGER = Logger.getLogger("me.xuxiaoxiao.chatapi.wechat");
	private static final Pattern REX_GROUPMSG = Pattern.compile("(@[0-9a-zA-Z]+):<br/>([\\s\\S]*)");
	private static final Pattern REX_REVOKE_ID = Pattern.compile("&lt;msgid&gt;(\\d+)&lt;/msgid&gt;");
	private static final Pattern REX_REVOKE_REPLACE = Pattern
			.compile("&lt;replacemsg&gt;&lt;!\\[CDATA\\[([\\s\\S]*)\\]\\]&gt;&lt;/replacemsg&gt;");

	private final WeChatThread wxThread = new WeChatThread();
	private final WeChatContacts wxContacts = new WeChatContacts();
	private final WeChatApi wxAPI;
	private final WeChatListener wxListener;

	public WeChatClient(WeChatListener wxListener)
	{
		this(wxListener, null, null);
	}

	public File getFolder()
	{
		return wxAPI.folder;
	}

	/**
	 * 
	 * @Title: setFolder   
	 * @Description: 设置文件存储位置
	 * @param: @param folder      
	 * @return: void      
	 * @throws
	 */
	public void setFolder(File folder)
	{
		wxAPI.folder = folder;
	}

	public WeChatClient(WeChatListener wxListener, File folder, Handler handler)
	{
		Objects.requireNonNull(wxListener);
		this.wxListener = wxListener;
		if (folder == null)
		{
			folder = new File("");
		}
		if (handler == null)
		{
			handler = new ConsoleHandler();
			handler.setLevel(Level.FINER);
		}
		this.wxAPI = new WeChatApi(folder);
		LOGGER.setLevel(handler.getLevel());
		LOGGER.setUseParentHandlers(false);
		LOGGER.addHandler(handler);
	}

	/**
	 * 启动客户端，注意：一个客户端类的实例只能被启动一次
	 */
	public void startup()
	{
		wxThread.start();
	}

	/**
	 * 客户端是否正在运行
	 *
	 * @return 是否正在运行
	 */
	public boolean isWorking()
	{
		return !wxThread.isInterrupted();
	}

	/**
	 * 关闭客户端，注意：关闭后的客户端不能再被启动
	 */
	public void shutdown()
	{
		wxAPI.webwxlogout();
		wxThread.interrupt();
	}

	/**
	 * 获取当前登录的用户信息
	 *
	 * @return 当前登录的用户信息
	 */
	public WXUser userMe()
	{
		return wxContacts.getMe();
	}

	/**
	 * 根据userId获取用户好友
	 *
	 * @param userId 好友的id
	 * @return 好友的信息
	 */
	public WXUser userFriend(String userId)
	{
		return wxContacts.getFriend(userId);
	}

	/**
	 * 获取用户所有好友
	 *
	 * @return 用户所有好友
	 */
	public HashMap<String, WXUser> userFriends()
	{
		return wxContacts.getFriends();
	}

	/**
	 * 根据群id获取群信息
	 *
	 * @param groupId 群id
	 * @return 群信息
	 */
	public WXGroup userGroup(String groupId)
	{
		return wxContacts.getGroup(groupId);
	}

	/**
	 * 获取用户所有群
	 *
	 * @return 用户所有群
	 */
	public HashMap<String, WXGroup> userGroups()
	{
		return wxContacts.getGroups();
	}

	/**
	 * 根据联系人id获取用户联系人信息
	 *
	 * @param contactId 联系人id
	 * @return 联系人信息
	 */
	public WXContact userContact(String contactId)
	{
		return wxContacts.getContact(contactId);
	}

	/**
	 * 发送文字消息
	 *
	 * @param wxContact 目标联系人
	 * @param text      要发送的文字
	 * @return 文本消息
	 */
	public WXText sendText(WXContact wxContact, String text)
	{
		LOGGER.info(String.format("向（%s）发送消息：%s", wxContact.id, text));
		RspSendMsg rspSendMsg = wxAPI.webwxsendmsg(
				new ReqSendMsg.Msg(RspSync.AddMsg.TYPE_TEXT, null, 0, text, null, wxContacts.getMe().id, wxContact.id));

		WXText wxText = new WXText();
		wxText.id = Long.valueOf(rspSendMsg.MsgID);
		wxText.idLocal = Long.valueOf(rspSendMsg.LocalID);
		wxText.timestamp = System.currentTimeMillis();
		wxText.fromGroup = null;
		wxText.fromUser = wxContacts.getMe();
		wxText.toContact = wxContact;
		wxText.content = text;
		return wxText;
	}

	/**
	 * 发送文件消息，可以是图片，动图，视频，文本等文件
	 *
	 * @param wxContact 目标联系人
	 * @param file      要发送的文件
	 * @return 图像或附件消息
	 */
	public WXMessage sendFile(WXContact wxContact, File file)
	{
		String suffix = WeChatTools.fileSuffix(file);
		if ("mp4".equals(suffix) && file.length() >= 20L * 1024L * 1024L)
		{
			LOGGER.warning(String.format("向（%s）发送的视频文件大于20M，无法发送", wxContact.id));
			return null;
		}
		else
		{
			try
			{
				LOGGER.info(String.format("向（%s）发送文件：%s", wxContact.id, file.getAbsolutePath()));
				String mediaId = null, aesKey = null, signature = null;
				if (file.length() >= 25L * 1024L * 1024L)
				{
					RspCheckUpload rspCheckUpload = wxAPI.webwxcheckupload(file, wxContacts.getMe().id, wxContact.id);
					mediaId = rspCheckUpload.MediaId;
					aesKey = rspCheckUpload.AESKey;
					signature = rspCheckUpload.Signature;
				}
				if (XTools.strEmpty(mediaId))
				{
					RspUploadMedia rspUploadMedia = wxAPI.webwxuploadmedia(wxContacts.getMe().id, wxContact.id, file,
							aesKey, signature);
					mediaId = rspUploadMedia.MediaId;
				}

				if (!XTools.strEmpty(mediaId))
				{
					switch (WeChatTools.fileType(file))
					{
					case "pic":
					{
						RspSendMsg rspSendMsg = wxAPI.webwxsendmsgimg(new ReqSendMsg.Msg(RspSync.AddMsg.TYPE_IMAGE,
								mediaId, null, "", signature, wxContacts.getMe().id, wxContact.id));
						WXImage wxImage = new WXImage();
						wxImage.id = Long.valueOf(rspSendMsg.MsgID);
						wxImage.idLocal = Long.valueOf(rspSendMsg.LocalID);
						wxImage.timestamp = System.currentTimeMillis();
						wxImage.fromGroup = null;
						wxImage.fromUser = wxContacts.getMe();
						wxImage.toContact = wxContact;
						wxImage.imgWidth = 0;
						wxImage.imgHeight = 0;
						wxImage.image = wxAPI.webwxgetmsgimg(wxImage.id, "slave");
						wxImage.origin = file;
						return wxImage;
					}
					case "video":
					{
						RspSendMsg rspSendMsg = wxAPI.webwxsendvideomsg(new ReqSendMsg.Msg(RspSync.AddMsg.TYPE_VIDEO,
								mediaId, null, "", signature, wxContacts.getMe().id, wxContact.id));
						WXVideo wxVideo = new WXVideo();
						wxVideo.id = Long.valueOf(rspSendMsg.MsgID);
						wxVideo.idLocal = Long.valueOf(rspSendMsg.LocalID);
						wxVideo.timestamp = System.currentTimeMillis();
						wxVideo.fromGroup = null;
						wxVideo.fromUser = wxContacts.getMe();
						wxVideo.toContact = wxContact;
						wxVideo.imgWidth = 0;
						wxVideo.imgHeight = 0;
						wxVideo.image = wxAPI.webwxgetmsgimg(wxVideo.id, "slave");
						wxVideo.videoLength = 0;
						wxVideo.video = file;
						return wxVideo;
					}
					default:
						if ("gif".equals(suffix))
						{
							RspSendMsg rspSendMsg = wxAPI
									.webwxsendemoticon(new ReqSendMsg.Msg(RspSync.AddMsg.TYPE_EMOJI, mediaId, 2, "",
											signature, wxContacts.getMe().id, wxContact.id));
							WXImage wxImage = new WXImage();
							wxImage.id = Long.valueOf(rspSendMsg.MsgID);
							wxImage.idLocal = Long.valueOf(rspSendMsg.LocalID);
							wxImage.timestamp = System.currentTimeMillis();
							wxImage.fromGroup = null;
							wxImage.fromUser = wxContacts.getMe();
							wxImage.toContact = wxContact;
							wxImage.imgWidth = 0;
							wxImage.imgHeight = 0;
							wxImage.image = file;
							wxImage.origin = file;
							return wxImage;
						}
						else
						{
							StringBuilder sbAppMsg = new StringBuilder();
							sbAppMsg.append("<appmsg appid='wxeb7ec651dd0aefa9' sdkver=''>");
							sbAppMsg.append("<title>").append(file.getName()).append("</title>");
							sbAppMsg.append("<des></des>");
							sbAppMsg.append("<action></action>");
							sbAppMsg.append("<type>6</type>");
							sbAppMsg.append("<content></content>");
							sbAppMsg.append("<url></url>");
							sbAppMsg.append("<lowurl></lowurl>");
							sbAppMsg.append("<appattach>");
							sbAppMsg.append("<totallen>").append(file.length()).append("</totallen>");
							sbAppMsg.append("<attachid>").append(mediaId).append("</attachid>");
							sbAppMsg.append("<fileext>").append(XTools.strEmpty(suffix) ? "undefined" : suffix)
									.append("</fileext>");
							sbAppMsg.append("</appattach>");
							sbAppMsg.append("<extinfo></extinfo>");
							sbAppMsg.append("</appmsg>");
							RspSendMsg rspSendMsg = wxAPI.webwxsendappmsg(new ReqSendMsg.Msg(6, null, null,
									sbAppMsg.toString(), signature, wxContacts.getMe().id, wxContact.id));
							WXFile wxFile = new WXFile();
							wxFile.id = Long.valueOf(rspSendMsg.MsgID);
							wxFile.idLocal = Long.valueOf(rspSendMsg.LocalID);
							wxFile.timestamp = System.currentTimeMillis();
							wxFile.fromGroup = null;
							wxFile.fromUser = wxContacts.getMe();
							wxFile.toContact = wxContact;
							wxFile.content = sbAppMsg.toString();
							wxFile.fileSize = file.length();
							wxFile.fileName = file.getName();
							wxFile.fileId = mediaId;
							wxFile.file = file;
							return wxFile;
						}
					}
				}
				else
				{
					LOGGER.severe(String.format("向（%s）发送的文件发送失败", wxContact.id));
				}
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * 获取用户头像
	 *
	 * @param wxContact 要获取头像文件的用户
	 * @return 获取头像文件后的用户
	 */
	public WXContact fetchAvatar(WXContact wxContact)
	{
		wxContact.avatarFile = XTools.http(wxAPI.httpOption, XRequest.GET(wxContact.avatarUrl))
				.file(wxAPI.folder.getAbsolutePath() + File.separator
						+ String.format("avatar-%d.jpg", System.currentTimeMillis() + new Random().nextInt(1000)));
		return wxContact;
	}

	/**
	 * 获取图片消息的大图
	 *
	 * @param wxImage 要获取大图的图片消息
	 * @return 获取大图后的图片消息
	 */
	public WXImage fetchImage(WXImage wxImage)
	{
		wxImage.origin = wxAPI.webwxgetmsgimg(wxImage.id, "big");
		return wxImage;
	}

	/**
	 * 获取语音消息的语音文件
	 *
	 * @param wxVoice 语音消息
	 * @return 获取语音文件后的语音消息
	 */
	public WXVoice fetchVoice(WXVoice wxVoice)
	{
		wxVoice.voice = wxAPI.webwxgetvoice(wxVoice.id);
		return wxVoice;
	}

	/**
	 * 获取视频消息的视频文件
	 *
	 * @param wxVideo 视频消息
	 * @return 获取视频文件后的视频消息
	 */
	public WXVideo fetchVideo(WXVideo wxVideo)
	{
		wxVideo.video = wxAPI.webwxgetvideo(wxVideo.id);
		return wxVideo;
	}

	/**
	 * 获取文件消息的附件文件
	 *
	 * @param wxFile 文件消息
	 * @return 获取附件文件后的文件消息
	 */
	public WXFile fetchFile(WXFile wxFile)
	{
		wxFile.file = wxAPI.webwxgetmedia(wxFile.id, wxFile.fileName, wxFile.fileId, wxFile.fromUser.id);
		return wxFile;
	}

	/**
	 * 撤回消息
	 *
	 * @param wxMessage 需要撤回的微信消息
	 */
	public void revokeMsg(WXMessage wxMessage)
	{
		LOGGER.info(String.format("撤回向（%s）发送的消息：%s，%s", wxMessage.toContact.id, wxMessage.idLocal, wxMessage.id));
		wxAPI.webwxrevokemsg(wxMessage.idLocal, wxMessage.id, wxMessage.toContact.id);
	}

	/**
	 * 同意好友申请
	 *
	 * @param wxVerify 好友验证消息
	 */
	public void passVerify(WXVerify wxVerify)
	{
		LOGGER.info(String.format("通过好友（%s）申请", wxVerify.userId));
		wxAPI.webwxverifyuser(3, wxVerify.userId, wxVerify.ticket, "");
	}

	/**
	 * 修改用户备注名
	 *
	 * @param wxUser 目标用户
	 * @param remark 备注名称
	 */
	public void editRemark(WXUser wxUser, String remark)
	{
		LOGGER.info(String.format("修改（%s）的备注：%s", wxUser.id, remark));
		wxAPI.webwxoplog(wxUser.id, remark);
	}

	/**
	 * 添加聊天室的成员
	 *
	 * @param wxGroup 目标聊天室
	 * @param userIds 要添加的人员的id，必须是自己的好友
	 */
	public void addGroupMember(WXGroup wxGroup, List<String> userIds)
	{
		LOGGER.info(String.format("为群（%s）添加成员：%s", wxGroup.id, userIds));
		wxAPI.webwxupdatechartroom(wxGroup.id, "addmember", userIds);
	}

	/**
	 * 移除聊天室的成员
	 *
	 * @param wxGroup 目标聊天室
	 * @param userIds 要移除的人员的id，必须是聊天室的成员，而且自己是管理员
	 */
	public void delGroupMember(WXGroup wxGroup, List<String> userIds)
	{
		LOGGER.info(String.format("为群（%s）删除成员：%s", wxGroup.id, userIds));
		wxAPI.webwxupdatechartroom(wxGroup.id, "delmember", userIds);
	}

	/**
	 * 模拟网页微信客户端监听器
	 */
	public static abstract class WeChatListener
	{
		/**
		 * 获取到用户登录的二维码
		 *
		 * @param qrCode 用户登录二维码的url
		 */
		public abstract void onQRCode(String qrCode);

		/**
		 * 获取用户头像，base64编码
		 *
		 * @param base64Avatar base64编码的用户头像
		 */
		public void onAvatar(String base64Avatar)
		{
		}

		/**
		 * 模拟网页微信客户端异常退出
		 *
		 * @param reason 错误原因
		 */
		public void onFailure(String reason)
		{
		}

		/**
		 * 用户登录并初始化成功
		 */
		public void onLogin()
		{
		}

		/**
		 * 用户获取到消息
		 *
		 * @param message 用户获取到的消息
		 */
		public void onMessage(WXMessage message)
		{
		}

		/**
		 * 联系人改变时候
		 */
		public void onModContact()
		{
		}

		/**
		 * 模拟网页微信客户端正常退出
		 */
		public void onLogout()
		{
		}
	}

	/**
	 * 模拟网页微信客户端工作线程
	 */
	private class WeChatThread extends Thread
	{

		@Override
		public void run()
		{
			int loginCount = 0;
			while (!isInterrupted())
			{
				// 用户登录
				LOGGER.fine("正在登录");
				String loginErr = login();
				if (!XTools.strEmpty(loginErr))
				{
					LOGGER.severe(String.format("登录出现错误：%s", loginErr));
					wxListener.onFailure(loginErr);
					return;
				}
				// 用户初始化
				LOGGER.fine("正在初始化");
				String initErr = initial();
				if (!XTools.strEmpty(initErr))
				{
					LOGGER.severe(String.format("初始化出现错误：%s", initErr));
					wxListener.onFailure(initErr);
					return;
				}
				wxListener.onLogin();
				// 同步消息
				LOGGER.fine("正在监听消息");
				String listenErr = listen();
				if (!XTools.strEmpty(listenErr))
				{
					if (loginCount++ > 10)
					{
						wxListener.onFailure(listenErr);
						return;
					}
					else
					{
						continue;
					}
				}
				// 退出登录
				LOGGER.finer("正在退出登录");
				wxListener.onLogout();
				return;
			}
		}

		/**
		 * 用户登录
		 *
		 * @return 登录时异常原因，为null表示正常登录
		 */
		private String login()
		{
			try
			{
				if (XTools.strEmpty(wxAPI.sid))
				{
					String qrCode = wxAPI.jslogin();
					LOGGER.finer(String.format("等待扫描二维码：%s", qrCode));
					wxListener.onQRCode(qrCode);
					while (true)
					{
						RspLogin rspLogin = wxAPI.login();
						switch (rspLogin.code)
						{
						case 200:
							LOGGER.finer("已授权登录");
							wxAPI.webwxnewloginpage(rspLogin.redirectUri);
							return null;
						case 201:
							LOGGER.finer("已扫描二维码");
							wxListener.onAvatar(rspLogin.userAvatar);
							break;
						case 408:
							LOGGER.finer("等待授权登录");
							break;
						default:
							LOGGER.warning("登录超时");
							return LOGIN_TIMEOUT;
						}
					}
				}
				else
				{
					return null;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				LOGGER.warning(String.format("登录异常：%s", Arrays.toString(e.getStackTrace())));
				return LOGIN_EXCEPTION;
			}
		}

		/**
		 * 初始化
		 *
		 * @return 初始化异常原因，为null表示正常初始化
		 */
		private String initial()
		{
			try
			{
				LOGGER.finer("正在获取Cookie");
				for (HttpCookie cookie : wxAPI.httpOption.cookieManager.getCookieStore().getCookies())
				{
					if ("wxsid".equalsIgnoreCase(cookie.getName()))
					{
						wxAPI.sid = cookie.getValue();
					}
					else if ("wxuin".equalsIgnoreCase(cookie.getName()))
					{
						wxAPI.uin = cookie.getValue();
					}
					else if ("webwx_data_ticket".equalsIgnoreCase(cookie.getName()))
					{
						wxAPI.dataTicket = cookie.getValue();
					}
				}

				// 获取自身信息
				LOGGER.finer("正在获取自身信息");
				RspInit rspInit = wxAPI.webwxinit();
				wxContacts.setMe(wxAPI.host, rspInit.User);

				// 添加初始联系人，特殊账号等
				LOGGER.finer("正在添加初始联系人，特殊账号等");
				for (RspInit.User user : rspInit.ContactList)
				{
					wxContacts.putContact(wxAPI.host, user);
				}

				// 发送状态信息
				wxAPI.webwxstatusnotify(wxContacts.getMe().id, WXNotify.NOTIFY_INITED);

				// 获取好友、群、公众号列表
				LOGGER.finer("正在获取好友、群、公众号列表");
				RspGetContact rspGetContact = wxAPI.webwxgetcontact();
				for (RspInit.User user : rspGetContact.MemberList)
				{
					wxContacts.putContact(wxAPI.host, user);
				}

				// 获取最近联系人
				LOGGER.finer("正在获取最近联系人");
				LinkedList<ReqBatchGetContact.Contact> contacts = new LinkedList<>();
				if (!XTools.strEmpty(rspInit.ChatSet))
				{
					for (String userName : rspInit.ChatSet.split(","))
					{
						contacts.add(new ReqBatchGetContact.Contact(userName, ""));
					}
				}
				HashMap<String, WXGroup> userGroups = wxContacts.getGroups();
				for (Entry<String, WXGroup> entry : userGroups.entrySet())
				{
					WXGroup wg = entry.getValue();
					if (wg.members.isEmpty())
					{
						contacts.add(new ReqBatchGetContact.Contact(wg.id, ""));
					}
				}

				loadContacts(contacts);

				return null;
			}
			catch (Exception e)
			{
				e.printStackTrace();
				LOGGER.warning(String.format("初始化异常：%s", e.getMessage()));
				return INIT_EXCEPTION;
			}
		}

		/**
		 * 循环同步消息
		 *
		 * @return 同步消息的异常原因，为null表示正常结束
		 */
		private String listen()
		{
			int retryCount = 0;
			try
			{
				while (!isInterrupted())
				{
					RspSyncCheck rspSyncCheck;
					try
					{
						LOGGER.finer("正在监听信息");
						rspSyncCheck = wxAPI.synccheck();
					}
					catch (Exception e)
					{
						e.printStackTrace();
						if (retryCount++ < 5)
						{
							LOGGER.warning(
									String.format("监听异常，重试第%d次：\n%s", retryCount, Arrays.toString(e.getStackTrace())));
							continue;
						}
						else
						{
							LOGGER.severe(String.format("监听异常，重试次数过多：\n%s", Arrays.toString(e.getStackTrace())));
							return LISTEN_EXCEPTION;
						}
					}
					retryCount = 0;
					if (rspSyncCheck.retcode > 0)
					{
						LOGGER.finer(String.format("停止监听信息：%d", rspSyncCheck.retcode));
						return null;
					}
					else if (rspSyncCheck.selector > 0)
					{
						RspSync rspSync = wxAPI.webwxsync();
						if (rspSync.DelContactList != null)
						{
							// 删除好友，删除群后的任意一条消息
							for (RspInit.User user : rspSync.DelContactList)
							{
								LOGGER.finer(String.format("删除联系人（%s）", user.UserName));
								wxContacts.rmvContact(user.UserName);
							}
						}
						if (rspSync.ModContactList != null)
						{
							// 被拉入群第一条消息，群里有人加入,群里踢人之后第一条信息，添加好友
							for (RspInit.User user : rspSync.ModContactList)
							{
								LOGGER.finer(String.format("变更联系人（%s）", user.UserName));
								wxContacts.putContact(wxAPI.host, user);
								wxListener.onModContact();
							}

						}
						if (rspSync.ModChatRoomMemberList != null)
						{
							for (RspInit.User user : rspSync.ModChatRoomMemberList)
							{
								LOGGER.finer(String.format("变更群成员（%s）", user.UserName));
								wxContacts.putContact(wxAPI.host, user);
							}
						}
						if (rspSync.AddMsgList != null)
						{
							for (RspSync.AddMsg addMsg : rspSync.AddMsgList)
							{
								wxListener.onMessage(parseMessage(addMsg));
							}
						}
					}
				}
				return null;
			}
			catch (Exception e)
			{
				e.printStackTrace();
				LOGGER.warning(String.format("监听消息异常：\n%s", Arrays.toString(e.getStackTrace())));
				return LISTEN_EXCEPTION;
			}
		}

		/**
		 * 获取联系人信息
		 *
		 * @param contacts 要获取的联系人的列表
		 */
		private void loadContacts(List<ReqBatchGetContact.Contact> contacts)
		{
			if (contacts.size() > 50)
			{
				LinkedList<ReqBatchGetContact.Contact> temp = new LinkedList<>();
				for (ReqBatchGetContact.Contact contact : contacts)
				{
					temp.add(contact);
					if (temp.size() >= 50)
					{
						RspBatchGetContact rspBatchGetContact = wxAPI.webwxbatchgetcontact(contacts);
						for (RspInit.User user : rspBatchGetContact.ContactList)
						{
							wxContacts.putContact(wxAPI.host, user);
						}
						temp.clear();
					}
				}
				contacts = temp;
			}
			if (contacts.size() > 0)
			{
				RspBatchGetContact rspBatchGetContact = wxAPI.webwxbatchgetcontact(contacts);
				for (RspInit.User user : rspBatchGetContact.ContactList)
				{
					wxContacts.putContact(wxAPI.host, user);
				}
			}
		}

		private <T extends WXMessage> T parseCommon(RspSync.AddMsg msg, T message)
		{
			message.id = msg.MsgId;
			message.idLocal = msg.MsgId;
			message.timestamp = msg.CreateTime * 1000;
			if (msg.FromUserName.startsWith("@@"))
			{
				if (wxContacts.getGroup(msg.FromUserName) == null)
				{
					loadContacts(Collections.singletonList(new ReqBatchGetContact.Contact(msg.FromUserName, "")));
				}
				message.fromGroup = wxContacts.getGroup(msg.FromUserName);
				Matcher mGroupMsg = REX_GROUPMSG.matcher(msg.Content);
				if (mGroupMsg.matches())
				{
					if (wxContacts.getContact(mGroupMsg.group(1)) == null)
					{
						if (message.fromGroup == null)
						{
							message.fromUser = null;
						}
						else
						{
							List<ReqBatchGetContact.Contact> contacts = new LinkedList<>();
							for (Map.Entry<String, WXGroup.Member> entry : message.fromGroup.members.entrySet())
							{
								contacts.add(new ReqBatchGetContact.Contact(entry.getValue().id, ""));
							}
							loadContacts(contacts);
							message.fromUser = (WXUser) wxContacts.getContact(mGroupMsg.group(1));
						}
					}
					message.fromUser = (WXUser) wxContacts.getContact(mGroupMsg.group(1));
				}
				message.toContact = wxContacts.getContact(msg.ToUserName);
				if (message.fromUser != null)
				{
					message.content = mGroupMsg.group(2);
				}
				else
				{
					message.content = msg.Content;
				}
			}
			else
			{
				message.fromGroup = null;
				message.fromUser = (WXUser) wxContacts.getContact(msg.FromUserName);
				message.toContact = wxContacts.getContact(msg.ToUserName);
				message.content = msg.Content;
			}
			return message;
		}

		private WXMessage parseMessage(RspSync.AddMsg msg)
		{
			try
			{
				switch (msg.MsgType)
				{
				case RspSync.AddMsg.TYPE_TEXT:
				{
					if (msg.SubMsgType == 0)
					{
						return parseCommon(msg, new WXText());
					}
					else if (msg.SubMsgType == 48)
					{
						WXLocation wxLocation = parseCommon(msg, new WXLocation());
						wxLocation.locationName = wxLocation.content.substring(0, wxLocation.content.indexOf(':'));
						wxLocation.locationImage = String.format("https://%s%s", wxAPI.host,
								wxLocation.content.substring(wxLocation.content.indexOf(':') + ":<br/>".length()));
						wxLocation.locationUrl = msg.Url;
						return wxLocation;
					}
					break;
				}
				case RspSync.AddMsg.TYPE_IMAGE:
				{
					WXImage wxImage = parseCommon(msg, new WXImage());
					wxImage.imgWidth = msg.ImgWidth;
					wxImage.imgHeight = msg.ImgHeight;
					wxImage.image = wxAPI.webwxgetmsgimg(msg.MsgId, "slave");
					return wxImage;
				}
				case RspSync.AddMsg.TYPE_VOICE:
				{
					WXVoice wxVoice = parseCommon(msg, new WXVoice());
					wxVoice.voiceLength = msg.VoiceLength;
					return wxVoice;
				}
				case RspSync.AddMsg.TYPE_VERIFY:
				{
					WXVerify wxVerify = parseCommon(msg, new WXVerify());
					wxVerify.userId = msg.RecommendInfo.UserName;
					wxVerify.userName = msg.RecommendInfo.NickName;
					wxVerify.signature = msg.RecommendInfo.Signature;
					wxVerify.province = msg.RecommendInfo.Province;
					wxVerify.city = msg.RecommendInfo.City;
					wxVerify.gender = msg.RecommendInfo.Sex;
					wxVerify.verifyFlag = msg.RecommendInfo.VerifyFlag;
					wxVerify.ticket = msg.RecommendInfo.Ticket;
					return wxVerify;
				}
				case RspSync.AddMsg.TYPE_RECOMMEND:
				{
					WXRecommend wxRecommend = parseCommon(msg, new WXRecommend());
					wxRecommend.userId = msg.RecommendInfo.UserName;
					wxRecommend.userName = msg.RecommendInfo.NickName;
					wxRecommend.gender = msg.RecommendInfo.Sex;
					wxRecommend.signature = msg.RecommendInfo.Signature;
					wxRecommend.province = msg.RecommendInfo.Province;
					wxRecommend.city = msg.RecommendInfo.City;
					wxRecommend.verifyFlag = msg.RecommendInfo.VerifyFlag;
					return wxRecommend;
				}
				case RspSync.AddMsg.TYPE_VIDEO:
				{
					// 视频貌似可以分片，后期测试
					WXVideo wxVideo = parseCommon(msg, new WXVideo());
					wxVideo.imgWidth = msg.ImgWidth;
					wxVideo.imgHeight = msg.ImgHeight;
					wxVideo.videoLength = msg.PlayLength;
					wxVideo.image = wxAPI.webwxgetmsgimg(msg.MsgId, "slave");
					return wxVideo;
				}
				case RspSync.AddMsg.TYPE_EMOJI:
				{
					WXImage wxImage = parseCommon(msg, new WXImage());
					wxImage.imgWidth = msg.ImgWidth;
					wxImage.imgHeight = msg.ImgHeight;
					if (!XTools.strEmpty(msg.Content) && msg.HasProductId == 0)
					{
						wxImage.image = wxAPI.webwxgetmsgimg(msg.MsgId, "big");
						wxImage.origin = wxImage.image;
					}
					return wxImage;
				}
				case RspSync.AddMsg.TYPE_OTHER:
				{
					if (msg.AppMsgType == 2)
					{
						WXImage wxImage = parseCommon(msg, new WXImage());
						wxImage.imgWidth = msg.ImgWidth;
						wxImage.imgHeight = msg.ImgHeight;
						wxImage.image = wxAPI.webwxgetmsgimg(msg.MsgId, "big");
						wxImage.origin = wxImage.image;
						return wxImage;
					}
					else if (msg.AppMsgType == 5)
					{
						WXLink wxLink = parseCommon(msg, new WXLink());
						wxLink.linkName = msg.FileName;
						wxLink.linkUrl = msg.Url;
						return wxLink;
					}
					else if (msg.AppMsgType == 6)
					{
						WXFile wxFile = parseCommon(msg, new WXFile());
						wxFile.fileId = msg.MediaId;
						wxFile.fileName = msg.FileName;
						wxFile.fileSize = XTools.strEmpty(msg.FileSize) ? 0 : Long.valueOf(msg.FileSize);
						return wxFile;
					}
					else if (msg.AppMsgType == 8)
					{
						WXImage wxImage = parseCommon(msg, new WXImage());
						wxImage.imgWidth = msg.ImgWidth;
						wxImage.imgHeight = msg.ImgHeight;
						wxImage.image = wxAPI.webwxgetmsgimg(msg.MsgId, "big");
						wxImage.origin = wxImage.image;
						return wxImage;
					}
					else if (msg.AppMsgType == 2000)
					{
						return parseCommon(msg, new WXMoney());
					}
					break;
				}
				case RspSync.AddMsg.TYPE_NOTIFY:
				{
					WXNotify wxNotify = parseCommon(msg, new WXNotify());
					wxNotify.notifyCode = msg.StatusNotifyCode;
					wxNotify.notifyContact = msg.StatusNotifyUserName;
					return wxNotify;
				}
				case RspSync.AddMsg.TYPE_SYSTEM:
				{
					return parseCommon(msg, new WXSystem());
				}
				case RspSync.AddMsg.TYPE_REVOKE:
					WXRevoke wxRevoke = parseCommon(msg, new WXRevoke());
					Matcher idMatcher = REX_REVOKE_ID.matcher(wxRevoke.content);
					if (idMatcher.find())
					{
						wxRevoke.msgId = Long.valueOf(idMatcher.group(1));
					}
					Matcher replaceMatcher = REX_REVOKE_REPLACE.matcher(wxRevoke.content);
					if (replaceMatcher.find())
					{
						wxRevoke.msgReplace = replaceMatcher.group(1);
					}
					return wxRevoke;
				default:
					break;
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				LOGGER.warning("消息解析失败");
			}
			return parseCommon(msg, new WXUnknown());
		}
	}
}
