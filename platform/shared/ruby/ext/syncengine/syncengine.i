/* syncengine.i */
%module SyncEngine
%{
/* Put header files here or function declarations like below */
	extern void rho_sync_doSyncAllSources(int show_status_popup);
	#define dosync_source rho_sync_doSyncSource
	extern void rho_sync_doSyncSource(int source_id,int show_status_popup);
	#define dosync rho_sync_doSyncAllSources
	extern void rho_sync_lock();
	#define lock_sync_mutex rho_sync_lock
	extern void rho_sync_unlock();
	#define unlock_sync_mutex rho_sync_unlock
	extern void rho_sync_login(const char *login, const char *password, const char* callback);
	#define login rho_sync_login
	extern int rho_sync_logged_in();
	#define logged_in rho_sync_logged_in
	extern void rho_sync_logout();
	#define logout rho_sync_logout
	extern void rho_sync_stop();
	#define stop_sync rho_sync_stop
	extern void rho_sync_set_notification(int source_id, const char *url, char* params);
	#define set_notification rho_sync_set_notification
	extern void rho_sync_clear_notification(int source_id);
	#define clear_notification rho_sync_clear_notification
	extern void rho_sync_set_pollinterval(int interval);
	#define set_pollinterval rho_sync_set_pollinterval
	extern void rho_sync_set_syncserver(char* syncserver);
	#define set_syncserver rho_sync_set_syncserver
	#if !defined(bool)
	#define bool int
	#define true  1
	#define false 0
	#endif
%}

%typemap(default) bool show_status_popup {
 $1 = 1;
}
extern void dosync(bool show_status_popup);
extern void dosync_source(int source_id, bool show_status_popup);
extern void lock_sync_mutex();
extern void unlock_sync_mutex();
extern void login(const char *login, const char *password, const char* callback);
extern int logged_in();
extern void logout();
extern void stop_sync();
extern void set_notification(int source_id, const char *url, char* params);
extern void clear_notification(int source_id);
extern void set_pollinterval(int interval);
extern void set_syncserver(char* syncserver);
