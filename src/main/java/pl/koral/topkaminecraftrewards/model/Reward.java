package pl.koral.topkaminecraftrewards.model;

public enum Reward {
        DAILY, WEEKLY, MONTHLY;

        public String toSQLString(){
            switch (this){
                case DAILY:
                    return "LAST_DAILY_REWARD_TIME";
                case WEEKLY:
                    return "LAST_WEEKLY_REWARD_TIME";
                case MONTHLY:
                    return "LAST_MONTHLY_REWARD_TIME";
            }
        return null;
        }
}
