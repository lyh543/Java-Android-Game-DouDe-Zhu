package com.ai.poker;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 斗地主AI - 升级版
 * 包含更好的策略算法
 */
public class AIv2 {
    
    /**
     * 叫地主评分 (0-100)
     */
    public static int bidScore(List<Poker> pokers) {
        int score = 0;
        PokerSeg seg = new PokerSeg(pokers);
        
        // 大牌加分
        for (Poker p : pokers) {
            switch (p.getP()) {
                case P2: score += 3;
                case PXW: score += 8;  // 小王
                case PDW: score += 10; // 大王
                case PA: score += 2;
                case PK: score += 1;
            }
        }
        
        // 炸弹加分
        List<P> bombs = findBombs(pokers);
        score += bombs.size() * 15;
        
        // 火箭直接满分
        if (hasRocket(pokers)) {
            return 100;
        }
        
        // 牌型加分
        if (hasStraight(seg)) score += 10;
        if (hasPairs(seg)) score += 5;
        if (hasTriples(seg)) score += 5;
        
        // 牌少加分
        if (pokers.size() <= 10) score += 10;
        else if (pokers.size() <= 15) score += 5;
        
        // 有2或A加分
        int count2A = countCards(pokers, P.P2) + countCards(pokers, P.PA);
        if (count2A >= 4) score += 5;
        
        return Math.min(100, score);
    }
    
    /**
     * 是否应该叫地主
     */
    public static boolean shouldCallDizhu(List<Poker> pokers) {
        return bidScore(pokers) >= 50;
    }
    
    /**
     * 主动出牌 - 升级版策略
     */
    public static List<Poker> sendPokersActively(List<Poker> owner, DIZHU dz, PokerCounts pcs) {
        PokerGroup pokerGroup = optimize(types(new PokerSeg(owner)));
        
        int myCount = owner.size();
        int prevCount = pcs.getPrevious_counts();
        int nextCount = pcs.getNext_counts();
        
        boolean amIFarmer = (dz != DIZHU.MINE);
        
        // 剩1张 - 溜
        if (myCount == 1) {
            return getSmallestSingle(pokerGroup);
        }
        
        // 剩2张 - 溜
        if (myCount == 2) {
            return getSmallestPairOrSingle(pokerGroup);
        }
        
        // 剩炸弹 - 考虑炸
        if (myCount == 4) {
            PokerGroupEntry bomb = pokerGroup.getPokerGroupEntry(PokerGroupTypeEnum.BOMB);
            if (bomb != null && !bomb.getPokerSegGroup().isEmpty()) {
                if (shouldPlayBomb(myCount, Math.min(prevCount, nextCount), dz, amIFarmer)) {
                    return bomb.getPokerSegGroup().getLast();
                }
            }
        }
        
        // 根据位置选择策略
        int[] strategy = getStrategy(dz, myCount, prevCount, nextCount, amIFarmer);
        
        for (int orderIndex : strategy) {
            PokerGroupTypeEnum type = PokerGroupTypeEnum.getPokerGroupTypeNum(orderIndex);
            PokerGroupEntry entry = pokerGroup.getPokerGroupEntry(type);
            
            if (entry != null && !entry.getPokerSegGroup().isEmpty()) {
                List<Poker> result = entry.getPokerSegGroup().getLast();
                if (result != null && !result.isEmpty()) {
                    return result;
                }
            }
        }
        
        // 尝试炸弹
        PokerGroupEntry bomb = pokerGroup.getPokerGroupEntry(PokerGroupTypeEnum.BOMB);
        if (bomb != null && !bomb.getPokerSegGroup().isEmpty()) {
            return bomb.getPokerSegGroup().getLast();
        }
        
        // 尝试火箭
        PokerGroupEntry rocket = pokerGroup.getPokerGroupEntry(PokerGroupTypeEnum.ROCKET);
        if (rocket != null && !rocket.getPokerSegGroup().isEmpty()) {
            return rocket.getPokerSegGroup().getLast();
        }
        
        return null;
    }
    
    /**
     * 被动出牌 - 升级版策略
     */
    public static List<Poker> sendPokersNegatively(List<Poker> owner, List<Poker> prePokers, DIZHU dz) {
        PokerSeg prePokerSeg = new PokerSeg(prePokers);
        PokerGroupTypeEnum prePokerType = getPokerGroupTypeEnum(prePokerSeg);
        
        PokerGroup allPokerGroup = types(new PokerSeg(owner));
        PokerGroup optimizedAllPokerGroup = optimize(Common.copyPokerGroup(allPokerGroup));
        
        // 尝试压牌
        List<Poker> result = tryToBeat(prePokerType, prePokerSeg, allPokerGroup, optimizedAllPokerGroup);
        if (result != null) {
            return result;
        }
        
        // 压不住，考虑炸
        if (!PokerGroupTypeEnum.ROCKET.equals(prePokerType)) {
            PokerGroupEntry bombEntry = allPokerGroup.getPokerGroupEntry(PokerGroupTypeEnum.BOMB);
            if (bombEntry != null && !bombEntry.getPokerSegGroup().isEmpty()) {
                for (PokerSeg bomb : bombEntry.getPokerSegGroup()) {
                    if (Common.compare(bomb, prePokerSeg, PokerGroupTypeEnum.BOMB, prePokerType) > 0) {
                        return bomb;
                    }
                }
            }
        }
        
        // 火箭
        PokerGroupEntry rocketEntry = allPokerGroup.getPokerGroupEntry(PokerGroupTypeEnum.ROCKET);
        if (rocketEntry != null && !rocketEntry.getPokerSegGroup().isEmpty()) {
            if (PokerGroupTypeEnum.BOMB.equals(prePokerType) || PokerGroupTypeEnum.ROCKET.equals(prePokerType)) {
                return rocketEntry.getPokerSegGroup().getLast();
            }
        }
        
        return null;
    }
    
    // ========== 辅助方法 ==========
    
    private static int[] getStrategy(DIZHU dz, int myCount, int prevCount, int nextCount, boolean amIFarmer) {
        // 地主策略 - 尽快跑
        if (dz == DIZHU.MINE) {
            if (myCount <= 6) {
                return new int[]{5, 4, 2, 1, 3, 7, 8, 6, 9};
            }
            return new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
        }
        
        // 农民策略
        if (amIFarmer) {
            if (dz == DIZHU.PREVIOUS) {
                // 压地主
                if (prevCount <= 2) {
                    return new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
                }
                return new int[]{2, 1, 3, 5, 4, 6, 7, 8, 9};
            }
            
            if (dz == DIZHU.NEXT) {
                // 配合上家
                if (nextCount <= 4) {
                    return new int[]{1, 2, 5, 4, 3, 6, 7, 8, 9};
                }
                return new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
            }
        }
        
        return new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9};
    }
    
    private static boolean shouldPlayBomb(int myCardCount, int opponentCardCount, DIZHU dz, boolean amIFarmer) {
        if (myCardCount >= 15) return true;
        if (opponentCardCount <= 2) return true;
        if (amIFarmer && dz == DIZHU.NEXT && opponentCardCount <= 4) return true;
        return false;
    }
    
    private static List<Poker> tryToBeat(PokerGroupTypeEnum preType, PokerSeg preSeg, 
            PokerGroup allGroup, PokerGroup optimizedGroup) {
        if (preType == null || PokerGroupTypeEnum.ROCKET.equals(preType)) {
            return null;
        }
        
        PokerGroupEntry entry = allGroup.getPokerGroupEntry(preType);
        PokerGroupEntry optimizedEntry = optimizedGroup.getPokerGroupEntry(preType);
        
        if (entry != null) {
            for (PokerSeg seg : entry.getPokerSegGroup()) {
                if (Common.compare(seg, preSeg, entry.getPokerGroupTypeEnum(), preType) > 0) {
                    return seg;
                }
            }
            
            if (optimizedEntry != null) {
                for (PokerSeg seg : optimizedEntry.getPokerSegGroup()) {
                    if (Common.compare(seg, preSeg, optimizedEntry.getPokerGroupTypeEnum(), preType) > 0) {
                        return seg;
                    }
                }
            }
        }
        
        return null;
    }
    
    private static List<Poker> getSmallestSingle(PokerGroup pokerGroup) {
        PokerGroupEntry entry = pokerGroup.getPokerGroupEntry(PokerGroupTypeEnum.DAN_PAI);
        if (entry != null && !entry.getPokerSegGroup().isEmpty()) {
            return entry.getPokerSegGroup().getLast();
        }
        return null;
    }
    
    private static List<Poker> getSmallestPairOrSingle(PokerGroup pokerGroup) {
        PokerGroupEntry pairEntry = pokerGroup.getPokerGroupEntry(PokerGroupTypeEnum.DUI_PAI);
        if (pairEntry != null && !pairEntry.getPokerSegGroup().isEmpty()) {
            return pairEntry.getPokerSegGroup().getLast();
        }
        return getSmallestSingle(pokerGroup);
    }
    
    private static List<P> findBombs(List<Poker> pokers) {
        List<P> bombs = new ArrayList<>();
        Set<P> seen = new HashSet<>();
        for (Poker p : pokers) {
            if (seen.contains(p.getP())) continue;
            int count = countCards(pokers, p.getP());
            if (count == 4) {
                bombs.add(p.getP());
                seen.add(p.getP());
            }
        }
        return bombs;
    }
    
    private static boolean hasRocket(List<Poker> pokers) {
        boolean hasXW = false, hasDW = false;
        for (Poker p : pokers) {
            if (p.getP() == P.PXW) hasXW = true;
            if (p.getP() == P.PDW) hasDW = true;
        }
        return hasXW && hasDW;
    }
    
    private static boolean hasStraight(PokerSeg seg) {
        // 简化的顺子检测
        return false;
    }
    
    private static boolean hasPairs(PokerSeg seg) {
        return seg.size() >= 4;
    }
    
    private static boolean hasTriples(PokerSeg seg) {
        return seg.size() >= 3;
    }
    
    private static int countCards(List<Poker> pokers, P p) {
        int count = 0;
        for (Poker pk : pokers) {
            if (pk.getP() == p) count++;
        }
        return count;
    }
    
    // 原有方法保持兼容
    public static int compare(List<Poker> pokera, List<Poker> pokerb) {
        return AI.compare(pokera, pokerb);
    }
    
    public static PokerGroupTypeEnum getPokerGroupTypeEnum(List<Poker> pokers) {
        return AI.getPokerGroupTypeEnum(pokers);
    }
    
    public static PokerGroup types(PokerSeg pokers) {
        return AI.types(pokers);
    }
    
    public static PokerGroup optimize(PokerGroup pokerGroup) {
        return AI.optimize(pokerGroup);
    }
    
    public static List<Poker> HintPokersActively(List<Poker> candidatePokers, PokerCounts pcs) {
        return sendPokersActively(candidatePokers, DIZHU.NEXT, pcs);
    }
    
    public static List<Poker> HintPokersNegatively(List<Poker> owner, List<Poker> prePokers, DIZHU dz) {
        return sendPokersNegatively(owner, prePokers, dz);
    }
    
    public static int CanJiaBei(List<Poker> pokers) {
        return AI.CanJiaBei(pokers);
    }
    
    public static boolean CanOwnDIZHU(List<Poker> pokers) {
        return shouldCallDizhu(pokers);
    }
}
