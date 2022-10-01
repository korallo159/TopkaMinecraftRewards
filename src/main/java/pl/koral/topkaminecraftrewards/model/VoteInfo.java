package pl.koral.topkaminecraftrewards.model;

import org.bukkit.entity.Player;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class VoteInfo {

    private UUID uuid;
    private int daysInARow;
    private long lastDailyRewardTime;
    private long lastWeeklyRewardTime;
    private long lastMonthlyRewardTime;

    public VoteInfo(UUID uuid, int daysInARow, long lastDailyRewardTime, long lastWeeklyRewardTime, long lastMonthlyRewardTime) {
        this.uuid = uuid;
        this.daysInARow = daysInARow;
        this.lastDailyRewardTime = lastDailyRewardTime;
        this.lastWeeklyRewardTime = lastWeeklyRewardTime;
        this.lastMonthlyRewardTime = lastMonthlyRewardTime;
    }

    public boolean didVote(Player player, Reward reward){
        long lastRewarded = 0;
        long daysInARow;

        switch (reward){
            case DAILY:
                lastRewarded = this.lastDailyRewardTime;
                break;

            case WEEKLY:
                lastRewarded = this.lastWeeklyRewardTime;
                break;

            case MONTHLY:
                lastRewarded = this.lastMonthlyRewardTime;
                break;


        }
        daysInARow = this.getDaysInARow();
        LocalDateTime last_rewarded;
        LocalDateTime now = Instant.now().atZone(ZoneId.systemDefault()).toLocalDateTime();
        switch (reward) {
            case DAILY:
                last_rewarded = Instant.ofEpochMilli(lastRewarded).atZone(ZoneId.systemDefault()).toLocalDateTime().plusDays(1);
                return last_rewarded.isAfter(now);
            case WEEKLY:
                last_rewarded = Instant.ofEpochMilli(lastRewarded).atZone(ZoneId.systemDefault()).toLocalDateTime().plusDays(7);
                return last_rewarded.isAfter(now) || daysInARow < 7;
            case MONTHLY:
                last_rewarded = Instant.ofEpochMilli(lastRewarded).atZone(ZoneId.systemDefault()).toLocalDateTime().plusDays(30);
                return last_rewarded.isAfter(now) || daysInARow < 30;
        }
        return false;

    }

    public long getPlayerLastVote(Reward reward){
        long lastVote = 0;
        switch (reward) {
            case DAILY:
                lastVote = this.lastDailyRewardTime;
                break;

            case WEEKLY:
                lastVote = this.lastWeeklyRewardTime;
                break;

            case MONTHLY:
                lastVote = this.lastMonthlyRewardTime;
                break;
        }

        return lastVote;
    }

    public boolean isInARow(Player player){

        LocalDateTime last_rewarded = Instant.ofEpochMilli(lastDailyRewardTime).atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime d1 = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault()).toLocalDateTime().minusDays(2);
        LocalDateTime d2 = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault()).toLocalDateTime().plusDays(2);

        return last_rewarded.isAfter(d1) && last_rewarded.isBefore(d2);

    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public int getDaysInARow() {
        return daysInARow;
    }

    public void setDaysInARow(int daysInARow) {
        this.daysInARow = daysInARow;
    }

    public long getLastDailyRewardTime() {
        return lastDailyRewardTime;
    }

    public void setLastDailyRewardTime(long lastDailyRewardTime) {
        this.lastDailyRewardTime = lastDailyRewardTime;
    }

    public long getLastWeeklyRewardTime() {
        return lastWeeklyRewardTime;
    }

    public void setLastWeeklyRewardTime(long lastWeeklyRewardTime) {
        this.lastWeeklyRewardTime = lastWeeklyRewardTime;
    }

    public long getLastMonthlyRewardTime() {
        return lastMonthlyRewardTime;
    }

    public void setLastMonthlyRewardTime(long lastMonthlyRewardTime) {
        this.lastMonthlyRewardTime = lastMonthlyRewardTime;
    }
}
