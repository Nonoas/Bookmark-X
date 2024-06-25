package indi.bookmarkx.common.data;

import indi.bookmarkx.common.data.index.SimpleColumnIndex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * @author: codeleep
 * @createTime: 2024/03/20 14:58
 * @description: 数组表格
 */
public class ArrayListTable<T> implements DataPool{

    protected List<T> dataList;

    protected final HashMap<Function, SimpleColumnIndex<Set<T>>> columnIndices = new HashMap<>();

    public ArrayListTable(List<T> arrayList, List<Function<T, Object>> functions) {
        this.dataList = arrayList;
        functions.forEach(this::addColumIndex);
    }

    protected void addColumIndex(Function<T, Object> function) {
        SimpleColumnIndex<Set<T>> index = new SimpleColumnIndex<>();
        dataList.forEach(data -> {
            try {
                saveHunt(data, function, index);
            }catch (Exception ignored) {

            }
        });
        columnIndices.put(function, index);
    }

    public List<T> getOnlyIndex(Object key){
        for (SimpleColumnIndex<Set<T>> index: columnIndices.values()) {
            Set<T> hunt = index.hunt(String.valueOf(key));
            if (hunt != null) {
                return new ArrayList<>(hunt);
            }
        }
        return null;
    }

    public void insert(T data){
        columnIndices.forEach((function, index) -> {
            saveHunt(data, function, (SimpleColumnIndex<Set<T>>) index);
        });
        this.dataList.add(data);
    }

    private void saveHunt(T data, Function function, SimpleColumnIndex<Set<T>> index) {
        String key = String.valueOf(function.apply(data));
        Set<T> hunt = index.hunt(key);
        if (hunt == null) {
            hunt = new HashSet<>();
            index.drawUp(key, hunt);
        }
        hunt.add(data);
    }

    public void delete(T data) {
        columnIndices.forEach((function, index) -> {
            String key = String.valueOf(function.apply(data));
            Set<T> hunt = index.hunt(key);
            if (hunt != null) {
                hunt.remove(data);
            }
        });
        this.dataList.remove(data);
    }
}
