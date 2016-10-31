package com.hly.easyretrofit.download.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * 下载
 * Created by houlianyong
 */
public class DownLoadDatabase {

    private final DatabaseHelper dbHelper;

    public DownLoadDatabase(Context context) {
        super();
        dbHelper = new DatabaseHelper(context);
    }

    /**
     * 下载缓存表
     */
    public static final class DownLoad {

        /**
         * 字段
         */
        public static final class Columns {
            public static final String ID = "_id";
            public static final String URL = "url";
            public static final String START = "start";
            public static final String END = "end";
            public static final String DOWNED = "downed";
            public static final String TOTAL = "total";
            public static final String SAVENAME = "saveName";
        }

        /**
         * 表名
         */
        public static final String TABLE_NAME = "kk_download";

        /**
         * 创建表的SQL
         */
        public static final String CREATE_SQL = "create table " + TABLE_NAME
                + " (" + Columns.ID + " integer primary key autoincrement," + Columns.URL + " varchar(200),"
                + Columns.START + " INTEGER," + Columns.END + " INTEGER," + Columns.TOTAL + " INTEGER," + Columns.DOWNED + " INTEGER," + Columns.SAVENAME + " varchar(200)" + ")";

        /**
         * 删除表的SQL
         */
        public static final String DROP_SQL = "drop table if exists " + TABLE_NAME;

        /**
         * 添加一条数据的SQL
         */
        public static final String INSERT_SQL = "insert into " + TABLE_NAME
                + " (" + Columns.URL + "," + Columns.START + "," + Columns.END + "," + Columns.TOTAL + "," + Columns.SAVENAME
                + ") values (?,?,?,?,?)";


        /**
         * 根据ID更新数据的SQL
         */
        public static final String UPDATE_SQL = "update " + TABLE_NAME +
                " set " + Columns.DOWNED + "=?  where " + Columns.ID + "=?";

        /**
         * 根据ID删除一条历史查询记录数据
         */
        public static final String DELETE_SQL = "delete from " + TABLE_NAME + " where _id=?";


        /**
         * 根据url，查询数据的SQL
         */
        public static final String QUERY_URL_SQL = "select * from " + TABLE_NAME
                + " where " + Columns.URL + "=?";

        /**
         * 根据url，start 查询数据的SQL
         */
        public static final String QUERY_URL_SQL_ID = "select * from " + TABLE_NAME
                + " where " + Columns.URL + "=? and " + Columns.START + "=?";

        /**
         * 删除所有数据的SQL
         */
        public static final String DELETE_ALL_SQL = "delete from " + TABLE_NAME;
    }

    /**
     * 插入数据
     */
    public synchronized DownLoadEntity insert(String url, int start, int end, int total, String saveName) {
        String sql = DownLoad.INSERT_SQL;
        SQLiteDatabase sqlite = dbHelper.getWritableDatabase();
        sqlite.execSQL(sql, new Object[]{url, start, end, total, saveName});
        DownLoadEntity entity = null;
        Cursor cursor = sqlite.rawQuery(DownLoad.QUERY_URL_SQL_ID, new String[]{url, String.valueOf(start)});
        if (cursor.moveToNext()) {
            entity = new DownLoadEntity();
            entity.url = cursor.getString(cursor.getColumnIndex(DownLoad.Columns.URL));
            entity.start = cursor.getInt(cursor.getColumnIndex(DownLoad.Columns.START));
            entity.end = cursor.getInt(cursor.getColumnIndex(DownLoad.Columns.END));
            entity.dataId = cursor.getInt(cursor.getColumnIndex(DownLoad.Columns.ID));
            entity.downed = cursor.getInt(cursor.getColumnIndex(DownLoad.Columns.DOWNED));
            entity.total = cursor.getInt(cursor.getColumnIndex(DownLoad.Columns.TOTAL));
            entity.saveName = cursor.getString(cursor.getColumnIndex(DownLoad.Columns.SAVENAME));
        }
        if (!cursor.isClosed()) {
            cursor.close();
        }
        sqlite.close();
        return entity;
    }

    /**
     * 更新一条历史查询记录
     */
    public synchronized void update(DownLoadEntity entity) {
        String sql = DownLoad.UPDATE_SQL;
        SQLiteDatabase sqlite = dbHelper.getWritableDatabase();
        sqlite.execSQL(sql, new Object[]{entity.downed, String.valueOf(entity.dataId)});
        sqlite.close();
    }

    public synchronized DownLoadEntity querySingle(String url, String start) {
        SQLiteDatabase sqlite = dbHelper.getWritableDatabase();
        DownLoadEntity entity = null;
        Cursor cursor = sqlite.rawQuery(DownLoad.QUERY_URL_SQL_ID, new String[]{url, start});
        if (cursor.moveToNext()) {
            entity = new DownLoadEntity();
            entity.url = cursor.getString(cursor.getColumnIndex(DownLoad.Columns.URL));
            entity.start = cursor.getInt(cursor.getColumnIndex(DownLoad.Columns.START));
            entity.end = cursor.getInt(cursor.getColumnIndex(DownLoad.Columns.END));
            entity.dataId = cursor.getInt(cursor.getColumnIndex(DownLoad.Columns.ID));
            entity.downed = cursor.getInt(cursor.getColumnIndex(DownLoad.Columns.DOWNED));
            entity.total = cursor.getInt(cursor.getColumnIndex(DownLoad.Columns.TOTAL));
            entity.saveName = cursor.getString(cursor.getColumnIndex(DownLoad.Columns.SAVENAME));
        }
        if (!cursor.isClosed()) {
            cursor.close();
        }
        sqlite.close();
        return entity;
    }

    /**
     * 删除 一条历史查询记录
     *
     * @param id
     */
    public synchronized void delete(int id) {
        SQLiteDatabase sqlite = dbHelper.getWritableDatabase();
        sqlite.execSQL(DownLoad.DELETE_SQL, new Integer[]{id});
        sqlite.close();
    }

    /**
     * 删除所有数据
     */
    public synchronized void deleteAll() {
        String sql = DownLoad.DELETE_ALL_SQL;
        SQLiteDatabase sqlite = dbHelper.getWritableDatabase();
        sqlite.execSQL(sql);
        sqlite.close();
    }

    public synchronized void deleteAllByUrl(String url) {
        List<DownLoadEntity> list = query(url);
        if (list.size() > 0) {
            Iterator iterator = list.iterator();
            while (iterator.hasNext()) {
                DownLoadEntity entity = (DownLoadEntity) iterator.next();
                delete(entity.dataId);
            }
        }
    }

    /**
     * 查询url的所有数据
     *
     * @return
     */
    public synchronized List<DownLoadEntity> query(String url) {
        if (TextUtils.isEmpty(url)) {
            url = "";
        }
        String sql = DownLoad.QUERY_URL_SQL;

        SQLiteDatabase sqlite = dbHelper.getReadableDatabase();
        ArrayList<DownLoadEntity> data = new ArrayList<>();
        Cursor cursor = sqlite.rawQuery(sql, new String[]{url});
        for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext()) {
            DownLoadEntity entity = new DownLoadEntity();
            entity.url = cursor.getString(cursor.getColumnIndex(DownLoad.Columns.URL));
            entity.start = cursor.getInt(cursor.getColumnIndex(DownLoad.Columns.START));
            entity.end = cursor.getInt(cursor.getColumnIndex(DownLoad.Columns.END));
            entity.dataId = cursor.getInt(cursor.getColumnIndex(DownLoad.Columns.ID));
            entity.downed = cursor.getInt(cursor.getColumnIndex(DownLoad.Columns.DOWNED));
            entity.total = cursor.getInt(cursor.getColumnIndex(DownLoad.Columns.TOTAL));
            entity.saveName = cursor.getString(cursor.getColumnIndex(DownLoad.Columns.SAVENAME));
            data.add(entity);
        }
        if (!cursor.isClosed()) {
            cursor.close();
        }
        sqlite.close();

        return data;
    }

    public synchronized void destroy() {
        dbHelper.close();
    }

}