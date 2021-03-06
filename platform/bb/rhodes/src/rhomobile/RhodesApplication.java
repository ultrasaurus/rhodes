package rhomobile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.io.HttpConnection;

import net.rim.device.api.browser.field.BrowserContent;
import net.rim.device.api.browser.field.BrowserContentChangedEvent;
import net.rim.device.api.browser.field.Event;
import net.rim.device.api.browser.field.RedirectEvent;
import net.rim.device.api.browser.field.RenderingApplication;
import net.rim.device.api.browser.field.RenderingException;
import net.rim.device.api.browser.field.RenderingOptions;
import net.rim.device.api.browser.field.RenderingSession;
import net.rim.device.api.browser.field.RequestedResource;
import net.rim.device.api.browser.field.UrlRequestedEvent;
import net.rim.device.api.io.http.HttpHeaders;
import net.rim.device.api.system.Alert;
import net.rim.device.api.system.Application;
import net.rim.device.api.system.ApplicationManager;
import net.rim.device.api.system.Characters;
import net.rim.device.api.system.KeyListener;
import net.rim.device.api.system.SystemListener;
//import javax.microedition.io.file.FileSystemListener;
import net.rim.device.api.system.TrackwheelListener;
import net.rim.device.api.ui.ContextMenu;
import net.rim.device.api.ui.Field;
import net.rim.device.api.ui.FieldChangeListener;
import net.rim.device.api.ui.Graphics;
import net.rim.device.api.ui.Keypad;
import net.rim.device.api.ui.MenuItem;
import net.rim.device.api.ui.UiApplication;
import net.rim.device.api.ui.component.Dialog;
import net.rim.device.api.ui.component.Menu;
import net.rim.device.api.ui.component.Status;
import net.rim.device.api.ui.container.MainScreen;
import net.rim.device.api.ui.container.PopupScreen;
import net.rim.device.api.ui.container.VerticalFieldManager;
//import net.rim.device.api.ui.container.HorizontalFieldManager;
import net.rim.device.api.ui.component.ButtonField;
import net.rim.device.api.ui.component.LabelField;
import net.rim.device.api.ui.Manager;

import javax.microedition.media.*;

import com.rho.RhoClassFactory;
import com.rho.RhoConf;
import com.rho.RhoEmptyLogger;
import com.rho.RhoLogger;
import com.rho.RhoRuby;
import com.rho.SimpleFile;
import com.rho.location.GeoLocation;
import com.rho.net.RhoConnection;
import com.rho.sync.SyncThread;
import com.rho.sync.ISyncStatusListener;
import com.rho.Jsr75File;

/**
 *
 */
final public class RhodesApplication extends UiApplication implements RenderingApplication, SystemListener, ISyncStatusListener//, FileSystemListener
{
	// Menu Labels
	public static final String LABEL_HOME = "Home";
	public static final String LABEL_REFRESH = "Refresh";
	public static final String LABEL_BACK = "Back";
	public static final String LABEL_SYNC = "Sync";
	public static final String LABEL_OPTIONS = "Options";
	public static final String LABEL_LOG = "Log";
	public static final String LABEL_SEPARATOR = "separator";
	public static final String LABEL_CLOSE = "Close";
	public static final String LABEL_NONE = "none";
	
	private static final RhoLogger LOG = RhoLogger.RHO_STRIP_LOG ? new RhoEmptyLogger() : 
		new RhoLogger("RhodesApplication");

	/*boolean m_bSDCardAdded = false;
	public void rootChanged(int arg0, String arg1)
	{
		LOG.INFO_OUT( "rootChanged. arg0 :" + arg0 + "arg1: " + arg1);
		if ( arg0 == FileSystemListener.ROOT_ADDED && arg1 != null &&
			  arg1.equals("SDCard/") )
			m_bSDCardAdded = true;
	}*/
	
	class CKeyListener  implements KeyListener{

		public boolean keyChar(char key, int status, int time) {
	        if( key == Characters.ENTER ) {
	        
	        	openLink();
	        	return true;
	        }
			return false;
		}

		public boolean keyDown(int keycode, int time) {
			int nKey = Keypad.key(keycode);
			if ( nKey == Keypad.KEY_ESCAPE )
			{
				back();
				return true;
			}

			return false;
		}

		public boolean keyRepeat(int keycode, int time) {return false;}
		public boolean keyStatus(int keycode, int time) {return false;}
		public boolean keyUp(int keycode, int time) {return false;}
    };

    class CTrackwheelListener implements TrackwheelListener{

		public boolean trackwheelClick(int status, int time) {
			openLink();
			return true;
		}

		public boolean trackwheelRoll(int amount, int status, int time) {return false;}
		public boolean trackwheelUnclick(int status, int time) {return false;}
    }

    /*class SyncNotificationsImpl extends SyncNotifications{
    	public void performNotification(String url, String body){

    		HttpHeaders headers = new HttpHeaders();
    		headers.addProperty("Content-Type", "application/x-www-form-urlencoded");
    		postUrl(url, body, headers);
*/
/*    		String curUrl = (String)_history.lastElement();
    		curUrl.replace('\\', '/');
    		if ( curUrl.equalsIgnoreCase(url) )
    			navigateUrl(curUrl);*/

  //  	}

    //}

    String canonicalizeURL( String url ){
		if ( url == null || url.length() == 0 )
			return "";

		url.replace('\\', '/');
		if ( !url.startsWith(_httpRoot) ){
    		if ( url.charAt(0) == '/' )
    			url = _httpRoot.substring(0, _httpRoot.length()-1) + url;
    		else
    			url = _httpRoot + url;
		}

		return url;
    }

    void navigateUrl(String url){
    	PrimaryResourceFetchThread thread = new PrimaryResourceFetchThread(
        		canonicalizeURL(url), null, null, null, this);
        thread.start();                       
    }
    
	public String getPathForMenuItem(String url) {
		url = _httpRoot +
			url.substring(url.charAt(0) == '\\' || url.charAt(0) == '/' ? 1 : 0 );
		return url;
	}
    
    void addMenuItem(String label, String value){
    	LOG.TRACE("Adding menu item: label: " + label + ", value: " + value);
    	_mainScreen.addCustomMenuItem(label, value);
    }
    
    void resetMenuItems() {
    	_mainScreen.setMenuItems(new Vector());
    }

    public void postUrl(String url, String body, HttpHeaders headers){
        PrimaryResourceFetchThread thread = new PrimaryResourceFetchThread(
        		canonicalizeURL(url), headers, body.getBytes(), null, this);
        thread.start();                       
    }

    void saveCurrentLocation(String url) {
    	if (RhoConf.getInstance().getBool("KeepTrackOfLastVisitedPage")) {
			RhoConf.getInstance().setString("LastVisitedPage",url);
			RhoConf.getInstance().saveToFile();
			LOG.TRACE("Saved LastVisitedPage: " + url);
		}   	
    }

    boolean restoreLocation() {
    	LOG.TRACE("Restore Location to LastVisitedPage");
    	if (RhoConf.getInstance().getBool("KeepTrackOfLastVisitedPage")) {
			String url = RhoConf.getInstance().getString("LastVisitedPage");
			if (url.length()>0) {
				LOG.TRACE("Navigating to LastVisitedPage: " + url);
				
				if ( _history.size() == 0 )
					_history.addElement(url);
				
				navigateUrl(url);
				return true;
			}
		} 
		return false;
    }
   
    void back(){
    	if ( _history.size() <= 1 )
    		return;

    	int nPos = _history.size()-2;
    	String url = (String)_history.elementAt(nPos);
    	_history.removeElementAt(nPos+1);

    	saveCurrentLocation(url);
    	
    	navigateUrl(url);
    }

    void addToHistory(String strUrl, String refferer ){
    	int nPos = -1;
    	for( int i = _history.size()-1; i >= 0; i-- ){
    		if ( strUrl.equalsIgnoreCase((String)_history.elementAt(i)) ){
    			nPos = i;
    			break;
    		}
    		/*String strUrl1 = strUrl + "/index";
    		if ( strUrl1.equalsIgnoreCase((String)_history.elementAt(i)) ){
    			nPos = i;
    			break;
    		}*/
    		if ( refferer != null && refferer.equalsIgnoreCase((String)_history.elementAt(i)) ){
    			nPos = i;
    			break;
    		}

    	}
    	if ( nPos == -1 ){
    		boolean bReplace = RhoConnection.findIndex(strUrl) != -1;

    		if ( bReplace )
    			_history.setElementAt(strUrl, _history.size()-1 );
    		else
    			_history.addElement(strUrl);
    	}
    	else{
    		_history.setSize(nPos+1);
    		_history.setElementAt(strUrl, _history.size()-1 );
    	}
    	saveCurrentLocation(strUrl);
    }

    void openLink(){
    	Menu menu = _mainScreen.getMenu(0);
        int size = menu.getSize();
        for(int i=0; i<size; i++)
        {
            MenuItem item = menu.getItem(i);
            String label = item.toString();
            if(label.equalsIgnoreCase("Get Link")) //TODO: catch by ID?
            {
              item.run();
            }
        }
//    	MenuItem item = _mainScreen.getSavedGetLinkItem();
//    	if ( item != null ) {
//    		item.run();
//    	}
    }

    public void showPopup(final String message) {
    	Application.getApplication().invokeLater(new Runnable() {
            public void run() {    	
            	String[] choices = { "Ok" };
            	requestForeground();
            	Dialog.ask(message,choices,0);
            }
    	});
    }
    
    public void vibrate(final String duration) {
    	Application.getApplication().invokeLater(new Runnable() {
            public void run() {    	
		    	int dt = 2500;
		    	try {
		    		dt = Integer.parseInt(duration);
		    	} catch (NumberFormatException e) {    		
		    	}
		    	
		    	if (dt > 25500) dt = 25500;
		    	
		    	if (dt > 0) {
		    		Alert.startVibrate(dt);
		    	}
            }
    	});
    }

    private static final String[][] filetypes = { {"mp3", "audio/mpeg"}, {"wav","audio/x-wav"} };
    private String getTypeFromExt(String file_name) {
    	int pt = file_name.lastIndexOf('.');
    	if (pt<0) {
    		return filetypes[0][1];
    	}
    	String ext = file_name.substring(pt+1);
    	for (int cnt = filetypes.length - 1; cnt >= 0; --cnt) {
    		if(filetypes[cnt][0].equals(ext)) {
    			return filetypes[cnt][1];
    		}
    	}
    	return null;
    }
    
    public void play_file(final String file_name, final String media_type) {
    	Application.getApplication().invokeLater(new Runnable() {
            public void run() {            	
            	String type = media_type == null ? getTypeFromExt(file_name) : media_type;
            	if (type != null) {
            		LOG.INFO("File type: " + type);
            	} else {
            		LOG.ERROR("Error - can't play unknown file type");
            		return;
            	}
            	
            	String types[] = 
            		javax.microedition.media.Manager.getSupportedContentTypes(null);
            	for (int cnt = types.length - 1; cnt >= 0; --cnt) {
            		if (type.equals(types[cnt])) {
            			LOG.INFO( "Playing file " + file_name + " of type: " + types[cnt]);
            			
            			SimpleFile file = null;
            			try {
            				//retrieve the file
            				Class clazz = Class.forName("rhomobile.RhodesApplication");
            				file = RhoClassFactory.createFile();
            				InputStream is = file.getResourceAsStream(clazz.getClass(), "/apps" + file_name);
            				//create an instance of the player from the InputStream
            				Player player = javax.microedition.media.Manager.createPlayer(is,type);
            				player.realize();
            				player.prefetch();
            				//start the player
            				player.start();
            	        } catch (Exception ex) { 
            	        	LOG.ERROR("Error playing " + file_name + " :" + ex.getMessage());
                        } finally {
                			try{
                				if ( file != null )
                					file.close();
                			}catch(Exception exc){}         	
                        }
            			return;
            		}
            	}
            	
            	LOG.ERROR("Error - media type " + type + " isn't supported.");
            }
    	});
    }
    
	private static final String REFERER = "referer";

    private RenderingSession _renderingSession;

    private CMainScreen _mainScreen = null;

    private SyncStatusPopup _syncStatusPopup = null;
    
    private String _lastStatusMessage = null;
    
    private HttpConnection  _currentConnection;

    private Vector _history;

    private final String _httpRoot = "http://localhost:8080/";

    private boolean _isFullBrowser = false;
    
    private static PushListeningThread _pushListeningThread = null;
    
    private static RhodesApplication _instance;

    public static RhodesApplication getInstance(){ return _instance; }
    /***************************************************************************
     * Main.
     **************************************************************************/
    public static void main(String[] args)
    {
    	//RhoLogger.InitRhoLog();
    	//LOG.TRACE("Rhodes MAIN started ***--------------------------***");
    	
    	//_pushListeningThread = new PushListeningThread();
    	//_pushListeningThread.start();
    	
		_instance = new RhodesApplication();
		_instance.enterEventDispatcher();
		_pushListeningThread.stop();
		
        RhoLogger.close();
		//LOG.TRACE("Rhodes MAIN exit ***--------------------------***");
    }

    void doClose(){   	
    	LOG.TRACE("Rhodes DO CLOSE ***--------------------------***");
/*    	
		SyncEngine.stop(null);
		GeoLocation.stop();
        RhoRuby.RhoRubyStop();
        
    	try{
    		RhoClassFactory.getNetworkAccess().close();
    	}catch(IOException exc){
    		LOG.ERROR(exc);
    	}
*/
    }

    boolean m_bActivated = false;
	public void activate() 
	{
		m_bActivated = true;
		doStartupWork();
		
    	LOG.TRACE("Rhodes activate ***--------------------------***");
//		SyncEngine.start(null);

        if(!restoreLocation()) {
        	navigateHome();
        }    
    	
		super.activate();
	}

	public void deactivate() {
    	LOG.TRACE("Rhodes deactivate ***--------------------------***");		
//		SyncEngine.stop(null);
		GeoLocation.stop();
		RingtoneManager.stop();

		super.deactivate();
	}

	synchronized public void setSyncStatusPopup(SyncStatusPopup popup) {
		_syncStatusPopup = popup;
		if (_syncStatusPopup != null) {
			_syncStatusPopup.showStatus(_lastStatusMessage);
		} else {
			_lastStatusMessage = null;
		}
	}
	
	synchronized public void reportStatus(String status, int error) {
		_lastStatusMessage = status;
		LOG.INFO("Sync status: " + status);
		if (_syncStatusPopup == null && error != 0) {
			createStatusPopup();
		} else if (_syncStatusPopup != null) { 
			_syncStatusPopup.showStatus(status);
		}
	}
	
	public void createStatusPopup() {
		invokeLater( new Runnable() {
			public void run() {
				if (_syncStatusPopup == null) {
					SyncStatusPopup popup = new SyncStatusPopup();
					RhodesApplication.getInstance().setSyncStatusPopup(popup);
					pushScreen(popup);
				}
			}
		});	
	}
	
	static class SyncStatusPopup extends PopupScreen {
		LabelField _labelStatus;
	    public SyncStatusPopup() {
	        super( new VerticalFieldManager( Manager.NO_VERTICAL_SCROLL | Manager.NO_VERTICAL_SCROLLBAR) );
			
	        add(_labelStatus = new LabelField("Synchronizing data...", Field.FIELD_HCENTER));
	        add(new LabelField(""));
	        
	        ButtonField hideButton = new ButtonField( "Hide", Field.FIELD_HCENTER );
			hideButton.setChangeListener( new HideListener(this) );
			add(hideButton);
	    }
	    
	    public void showStatus(String status) {
	    	if (status == null) return;
            synchronized (Application.getEventLock()) {	
	    		_labelStatus.setText(status);
            }
	    }
	    
	    protected boolean keyDown( int keycode, int status ) {
	        if ( Keypad.key( keycode ) == Keypad.KEY_ESCAPE ) {
	            close();
	            RhodesApplication.getInstance().setSyncStatusPopup(null);
	            return true;
	        }
	        return super.keyDown( keycode, status );
	    }
	    
		private class HideListener implements FieldChangeListener {
			SyncStatusPopup owner;
			public HideListener(SyncStatusPopup _owner) {
				super();
				owner = _owner;
			}
			public void fieldChanged(Field field, int context) {
				owner.close();
				RhodesApplication.getInstance().setSyncStatusPopup(null);
			}
		}
	}
	
    class CMainScreen extends MainScreen{
    	
    	private Vector menuItems = new Vector();

		private MenuItem homeItem = new MenuItem(RhodesApplication.LABEL_HOME, 200000, 10) {
			public void run() {
					navigateHome();
				}
			};
		private MenuItem refreshItem = new MenuItem(RhodesApplication.LABEL_REFRESH, 200000, 10) {
			public void run() {
					refreshCurrentPage();
				}
			};
		private MenuItem backItem = new MenuItem(RhodesApplication.LABEL_BACK, 200000, 10) {
			public void run() {
					back();
				}
			};
		private MenuItem syncItem = new MenuItem(RhodesApplication.LABEL_SYNC, 200000, 10) {
			public void run() {
					RhodesApplication.getInstance().createStatusPopup();		
					SyncThread.doSyncAllSources();
				}
			};
		private MenuItem optionsItem = new MenuItem(RhodesApplication.LABEL_OPTIONS, 200000, 10) {
			public void run() {
					String curUrl = RhoRuby.getOptionsPage();
					curUrl = getPathForMenuItem(curUrl);

					addToHistory(curUrl, null );
			    	
					navigateUrl(curUrl);
				}
			};
		private MenuItem logItem = new MenuItem(RhodesApplication.LABEL_LOG, 200000, 10) {
			public void run() {
					LogScreen screen = new LogScreen();
			        //Push this screen to display it to the user.
			        UiApplication.getUiApplication().pushScreen(screen);
				}
			};

		private MenuItem separatorItem = MenuItem.separator(200000);
		
		private MenuItem closeItem = new MenuItem(RhodesApplication.LABEL_CLOSE, 200000, 10) {
			public void run() {
					close();
				}
			};

		private MenuItem savedGetLinkItem = null;

		protected void makeMenu(Menu menu, int instance) {
	        // TODO: This is really a hack, we should replicate the "Get Link" functionality
			// Also, for some reason the menu size becomes 0 when there is 1 item left (page view)
	    	for(int i=0; i < menu.getSize(); i++) {
	    		System.out.println("Getting menu item: " + i);
	    	    MenuItem item = menu.getItem(i);
	    	    String label = item.toString();
	    	    // Save the get link menuitem
	    	    if(!label.equalsIgnoreCase("Get Link")) {
	    	    	//savedGetLinkItem = item;
	    	    	menu.deleteItem(i);
	                if ( i > 0 ) 
	                	i = i - 1;
	    	    } 
	    	    
	    	}
			// Delete Page View
	    	// TODO: menu.getSize() above incorrectly reports size 0 when
	    	// there is 1 item left!
	    	MenuItem pgview = menu.getItem(0);
	    	if (pgview != null && pgview.getId() == 853)
	    		menu.deleteItem(0);
	    	
			// Don't draw menu if menuItems is null
			if (menuItems == null)
				return;
	    	
			ContextMenu contextMenu = ContextMenu.getInstance();
	        contextMenu.clear();
	        
			// Draw default menu
			if (menuItems != null && menuItems.size() == 0) {
				setDefaultItemToMenu(RhodesApplication.LABEL_HOME, homeItem, contextMenu);
				setDefaultItemToMenu(RhodesApplication.LABEL_REFRESH, refreshItem, contextMenu);
				setDefaultItemToMenu(RhodesApplication.LABEL_SYNC, syncItem, contextMenu);
				setDefaultItemToMenu(RhodesApplication.LABEL_OPTIONS, optionsItem, contextMenu);
				setDefaultItemToMenu(RhodesApplication.LABEL_LOG, logItem, contextMenu);
				setDefaultItemToMenu(RhodesApplication.LABEL_SEPARATOR, separatorItem, contextMenu);
				setDefaultItemToMenu(RhodesApplication.LABEL_CLOSE, closeItem, contextMenu);
			}

			// Draw menu from rhodes framework
			Enumeration elements = menuItems.elements();
			while (elements.hasMoreElements()) {
				MenuItem item = (MenuItem)elements.nextElement();
				contextMenu.addItem(item);
			}
		
			this.makeContextMenu(contextMenu);
			menu.add(contextMenu);
		}
		
		public void addCustomMenuItem(String label, final String value) {
			// Is this a default item? If so, use the existing menu item we have.
    	    if (value.equalsIgnoreCase(RhodesApplication.LABEL_HOME)) {
    	    	setDefaultItemToMenuItems(label, homeItem);
    	    } else if (value.equalsIgnoreCase(RhodesApplication.LABEL_REFRESH)) {
    	    	setDefaultItemToMenuItems(label, refreshItem);
    	    } else if (value.equalsIgnoreCase(RhodesApplication.LABEL_BACK)) {
    	    	setDefaultItemToMenuItems(label, backItem);
    	    }  else if (value.equalsIgnoreCase(RhodesApplication.LABEL_SYNC)) {
    	    	setDefaultItemToMenuItems(label, syncItem);
    	    } else if (value.equalsIgnoreCase(RhodesApplication.LABEL_OPTIONS)) {
    	    	setDefaultItemToMenuItems(label, optionsItem);
    	    } else if (value.equalsIgnoreCase(RhodesApplication.LABEL_LOG)) {
    	    	setDefaultItemToMenuItems(label, logItem);
    	    } else if (label.equalsIgnoreCase(RhodesApplication.LABEL_SEPARATOR) || 
    	    		   (value != null && value.equalsIgnoreCase(RhodesApplication.LABEL_SEPARATOR))) {
    	    	menuItems.addElement(separatorItem);
    	    } else if (value.equalsIgnoreCase(RhodesApplication.LABEL_CLOSE)) {
    	    	setDefaultItemToMenuItems(label, closeItem);
    	    } else if (label.equalsIgnoreCase(RhodesApplication.LABEL_NONE)) {
    	    	menuItems = null;
    	    } else {
				MenuItem itemToAdd = new MenuItem(label, 200000, 10) {
					public void run() {
				    	String val = getPathForMenuItem(value);
						addToHistory(val, null );
						navigateUrl(val);
					}
				};
				menuItems.addElement(itemToAdd);
    	    }
		}

		private void setDefaultItemToMenuItems(String label, MenuItem item) {
			item.setText(label);
	    	menuItems.addElement(item);
		}
		
		private void setDefaultItemToMenu(String label, MenuItem item, ContextMenu menu) {
			item.setText(label);
	    	menu.addItem(item);
		}

		public void close() {
			LOG.TRACE("Calling Screen.close");
			Application.getApplication().requestBackground();
		}
		
		public boolean onClose() {
			doClose();
			return super.onClose();
			//System.exit(0);
			//return true;
		}

		public boolean onMenu(int instance) {
			// TODO Auto-generated method stub
			return super.onMenu(instance);
		}

		public Vector getMenuItems() {
			return menuItems;
		}

		public void setMenuItems(Vector menuItems) {
			this.menuItems = menuItems;
		}

		public MenuItem getSavedGetLinkItem() {
			return savedGetLinkItem;
		}
    }

    boolean isWaitForSDCardAtStartup()
    {
    	if ( Jsr75File.isRhoFolderExist() )
    		return false;
    	
    	if ( Jsr75File.isSDCardExist() )
    		return false;
    	
    	return !m_bActivated;
    }
    
    private void doStartupWork() 
    {
    	if (_mainScreen!=null)
    		return;
    	
    	if ( ApplicationManager.getApplicationManager().inStartup() || isWaitForSDCardAtStartup() )
    	{
            this.invokeLater( new Runnable() {
                public void run() 
                {
                    doStartupWork(); 
                }
            } );
            
            return;
    	}
    	
    	try{
        	RhoLogger.InitRhoLog();
	    	
	        LOG.INFO(" STARTING RHODES: ***----------------------------------*** " );
	    	
	    	CKeyListener list = new CKeyListener();
	    	CTrackwheelListener wheel = new CTrackwheelListener();
	    	this._history = new Vector();
	
	        //SyncEngine.setNotificationImpl( new SyncNotificationsImpl() );
	
	        _mainScreen = new CMainScreen();
	        _mainScreen.addKeyListener(list);
	        _mainScreen.addTrackwheelListener(wheel);
	
	        pushScreen(_mainScreen);
	        _renderingSession = RenderingSession.getNewInstance();
	        // enable javascript
	        _renderingSession.getRenderingOptions().setProperty(RenderingOptions.CORE_OPTIONS_GUID, RenderingOptions.JAVASCRIPT_ENABLED, true);
	        _renderingSession.getRenderingOptions().setProperty(RenderingOptions.CORE_OPTIONS_GUID, RenderingOptions.JAVASCRIPT_LOCATION_ENABLED, true);
	        _renderingSession.getRenderingOptions().setProperty(RenderingOptions.CORE_OPTIONS_GUID, RenderingOptions.ENABLE_CSS, true);
	        
	        if ( RhoConf.getInstance().getBool("use_bb_full_browser") )
	        {
		        com.rho.Jsr75File.SoftVersion ver = com.rho.Jsr75File.getSoftVersion();
		        if ( ver.nMajor == 4 && ver.nMinor == 6 )
		        {
			        //this is the undocumented option to tell the browser to use the 4.6 Rendering Engine
			        _renderingSession.getRenderingOptions().setProperty(RenderingOptions.CORE_OPTIONS_GUID, 17000, true);
		        	_isFullBrowser = true;
		        }
	        }
	        
	    	_pushListeningThread = new PushListeningThread();
	    	_pushListeningThread.start();
	        
	    	try {
	    		RhoClassFactory.getNetworkAccess().configure();
	    	} catch(IOException exc) {
	    		LOG.ERROR(exc.getMessage());
	    	}
	    	
	        RhoRuby.RhoRubyStart("");
	        SyncThread sync = SyncThread.Create( new RhoClassFactory() );
	        if (sync != null) {
	        	sync.setStatusListener(this);
	        }
	        
	        //Do it in onActivate
	        //if(!restoreLocation()) {
	        //	navigateHome();
	        //}    
	        
	        LOG.INFO("RHODES STARTUP COMPLETED: ***----------------------------------*** " );
    	}catch(Exception exc)
    	{
    		LOG.ERROR("doStartupWork failed", exc);
    	}
    }
    
    private void invokeStartupWork() {
        // I think this can get called twice
        // 1) Directly from startup, if the app starts while the BB is up - e.g. after download
        // 2) From System Listener - after system restart and when the app is originally installed
        // To make sure we don't actually do the startup stuff twice,
        // we use _mainScreen as a flag
        if ( _mainScreen == null ) {
            LOG.INFO_OUT(" Shedule doStartupWork() ***---------------------------------- " );
            this.invokeLater( new Runnable() { 
                public void run() 
                {
                    doStartupWork(); 
                }
            } );
        }
    }

    //----------------------------------------------------------------------
    // SystemListener methods

    public void powerUp() {
        LOG.INFO_OUT(" POWER UP ***----------------------------------*** " );
        invokeStartupWork();
        this.requestBackground();
    }
    public void powerOff() {
        LOG.TRACE(" POWER DOWN ***----------------------------------*** " );
//        _mainScreen = null;
//        doClose();
    }
    public void batteryLow() { }
    public void batteryGood() { }    
    public void batteryStatusChange(int status) { }

    // end of SystemListener methods
    //----------------------------------------------------------------------
    
    private RhodesApplication() {
        LOG.INFO_OUT(" Construct RhodesApplication() ***----------------------------------*** " );
        this.addSystemListener(this);
        //this.addFileSystemListener(this);
        if ( ApplicationManager.getApplicationManager().inStartup() ) {
            LOG.INFO_OUT("We are in the phone startup, don't start Rhodes yet, leave it to power up call");
        } else {
            invokeStartupWork();
        }
    }

    public void refreshCurrentPage(){
		navigateUrl(getCurrentPageUrl());
    }

    public String getCurrentPageUrl(){
    	return (String)_history.lastElement();
    }

    void navigateHome(){
    	String strHomePage = RhoRuby.getStartPage();
    	String strStartPage = _httpRoot;
    	if ( strHomePage != null && strHomePage.length() > 0 )
    	{
    		strStartPage = _httpRoot.substring(0, _httpRoot.length()-1) + strHomePage;
    	}
    	
        _history.removeAllElements();
	    _history.addElement(strStartPage);
	    navigateUrl(strStartPage);
    }

    public void processConnection(HttpConnection connection, Event e) {

        // cancel previous request
        if (_currentConnection != null) {
            try {
                _currentConnection.close();
            } catch (IOException e1) {
            }
        }

        _currentConnection = connection;

        BrowserContent browserContent = null;

        try {
            browserContent = _renderingSession.getBrowserContent(connection, this, e);

            if (browserContent != null) {

                Field field = browserContent.getDisplayableContent();
                if (field != null) {
                    synchronized (Application.getEventLock()) {
                        _mainScreen.deleteAll();
                        _mainScreen.add(field);
                    }
                }

                if ( _isFullBrowser )
                	browserContent.finishLoading();
                else
                {
	                synchronized (getAppEventLock())
	                {
	                	browserContent.finishLoading();
	                }
                }
            }

        } catch (RenderingException re) {

        } finally {
            SecondaryResourceFetchThread.doneAddingImages();
        }

    }

    /**
     * @see net.rim.device.api.browser.RenderingApplication#eventOccurred(net.rim.device.api.browser.Event)
     */
    public Object eventOccurred(Event event) {

        int eventId = event.getUID();

        switch (eventId) {

            case Event.EVENT_URL_REQUESTED : {

                UrlRequestedEvent urlRequestedEvent = (UrlRequestedEvent) event;
                String absoluteUrl = urlRequestedEvent.getURL();
                if ( !absoluteUrl.startsWith(_httpRoot) )
                	absoluteUrl = _httpRoot + absoluteUrl.substring(_httpRoot.length()-5);

                if ( urlRequestedEvent.getPostData() == null ||
                	 urlRequestedEvent.getPostData().length == 0 )
                	addToHistory(absoluteUrl, null );

                PrimaryResourceFetchThread thread = new PrimaryResourceFetchThread(absoluteUrl,
                                                                                   urlRequestedEvent.getHeaders(),
                                                                                   urlRequestedEvent.getPostData(),
                                                                                   event, this);
                thread.start();

                break;

            } case Event.EVENT_BROWSER_CONTENT_CHANGED: {

                // browser field title might have changed update title
                BrowserContentChangedEvent browserContentChangedEvent = (BrowserContentChangedEvent) event;

                if (browserContentChangedEvent.getSource() instanceof BrowserContent) {
                    BrowserContent browserField = (BrowserContent) browserContentChangedEvent.getSource();
                    String newTitle = browserField.getTitle();
                    if (newTitle != null) {
                        synchronized (getAppEventLock())
                        {
                        	_mainScreen.setTitle(newTitle);
                        }
                    }
                }

                break;

            } case Event.EVENT_REDIRECT : {

                    RedirectEvent e = (RedirectEvent) event;
                    String referrer = e.getSourceURL();
                    String absoluteUrl = e.getLocation();

                    switch (e.getType()) {

                        case RedirectEvent.TYPE_SINGLE_FRAME_REDIRECT :
                            // show redirect message
                            Application.getApplication().invokeAndWait(new Runnable() {
                                public void run() {
                                    Status.show("You are being redirected to a different page...");
                                }
                            });

                            break;

                        case RedirectEvent.TYPE_JAVASCRIPT :
                            break;
                        case RedirectEvent.TYPE_META :
                            // MSIE and Mozilla don't send a Referer for META Refresh.
                            referrer = null;
                            break;
                        case RedirectEvent.TYPE_300_REDIRECT :
                            // MSIE, Mozilla, and Opera all send the original
                            // request's Referer as the Referer for the new
                            // request.
                            if ( !absoluteUrl.startsWith(_httpRoot) )
                            	absoluteUrl = _httpRoot + absoluteUrl.substring(_httpRoot.length()-5);

                        	addToHistory(absoluteUrl,referrer);
                            Object eventSource = e.getSource();
                            if (eventSource instanceof HttpConnection) {
                                referrer = ((HttpConnection)eventSource).getRequestProperty(REFERER);
                            }
                            break;

                    }

                    HttpHeaders requestHeaders = new HttpHeaders();
                    requestHeaders.setProperty(REFERER, referrer);
                    PrimaryResourceFetchThread thread = new PrimaryResourceFetchThread(absoluteUrl, requestHeaders,null, event, this);
                    thread.start();
                    break;

            } case Event.EVENT_CLOSE :
                // TODO: close the application
                break;

            case Event.EVENT_SET_HEADER :        // no cache support
            case Event.EVENT_SET_HTTP_COOKIE :   // no cookie support
            case Event.EVENT_HISTORY :           // no history support
            case Event.EVENT_EXECUTING_SCRIPT :  // no progress bar is supported
            case Event.EVENT_FULL_WINDOW :       // no full window support
            case Event.EVENT_STOP :              // no stop loading support
            default :
        }

        return null;
    }

    /**
     * @see net.rim.device.api.browser.RenderingApplication#getAvailableHeight(net.rim.device.api.browser.BrowserContent)
     */
    public int getAvailableHeight(BrowserContent browserField) {
        // field has full screen
        return Graphics.getScreenHeight();
    }

    /**
     * @see net.rim.device.api.browser.RenderingApplication#getAvailableWidth(net.rim.device.api.browser.BrowserContent)
     */
    public int getAvailableWidth(BrowserContent browserField) {
        // field has full screen
        return Graphics.getScreenWidth();
    }

    /**
     * @see net.rim.device.api.browser.RenderingApplication#getHistoryPosition(net.rim.device.api.browser.BrowserContent)
     */
    public int getHistoryPosition(BrowserContent browserField) {
        // no history support
        return 0;
    }

    /**
     * @see net.rim.device.api.browser.RenderingApplication#getHTTPCookie(java.lang.String)
     */
    public String getHTTPCookie(String url) {
        // no cookie support
        return null;
    }

    /**
     * @see net.rim.device.api.browser.RenderingApplication#getResource(net.rim.device.api.browser.RequestedResource,
     *      net.rim.device.api.browser.BrowserContent)
     */
    public HttpConnection getResource( RequestedResource resource, BrowserContent referrer) {

        if (resource == null) {
            return null;
        }

        // check if this is cache-only request
        if (resource.isCacheOnly()) {
            // no cache support
            return null;
        }

        String url = resource.getUrl();

        if (url == null) {
            return null;
        }

        // if referrer is null we must return the connection
        if (referrer == null) {
            HttpConnection connection = Utilities.makeConnection(resource.getUrl(), resource.getRequestHeaders(), null);
            return connection;

        } else {

            // if referrer is provided we can set up the connection on a separate thread
            SecondaryResourceFetchThread.enqueue(resource, referrer);

        }

        return null;
    }

    /**
     * @see net.rim.device.api.browser.RenderingApplication#invokeRunnable(java.lang.Runnable)
     */
    public void invokeRunnable(Runnable runnable) {
        (new Thread(runnable)).start();
    }

    public static class PrimaryResourceFetchThread extends Thread {

        private RhodesApplication _application;

        private Event _event;

        private byte[] _postData;

        private HttpHeaders _requestHeaders;

        private String _url;
        private static Object m_syncObject = new Object(); 
        	
        public PrimaryResourceFetchThread(String url, HttpHeaders requestHeaders, byte[] postData,
                                      Event event, RhodesApplication application) {

            _url = url;
            _requestHeaders = requestHeaders;
            _postData = postData;
            _application = application;
            _event = event;
        }

        public void run() {
        	synchronized(m_syncObject)
        	{
        		HttpConnection connection = Utilities.makeConnection(_url, _requestHeaders, _postData);
        		_application.processConnection(connection, _event);
        	}
        }
    }
}

