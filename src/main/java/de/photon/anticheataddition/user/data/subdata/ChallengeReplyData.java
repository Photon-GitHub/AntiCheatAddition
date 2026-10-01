package de.photon.anticheataddition.user.data.subdata;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;

public final class ChallengeReplyData
{
    private static final int MAX_PENDING = 1024;
    private final Multiset<Long> sentIds = HashMultiset.create();

    /**
     * @return whether this issuance fits within the outstanding reply window.
     */
    public synchronized boolean issued(long id)
    {
        if (sentIds.size() >= MAX_PENDING) return false;
        sentIds.add(id);
        return true;
    }

    /**
     * @return whether an outstanding server challenge authorizes this reply.
     */
    public synchronized boolean replied(long id)
    {
        final int contained = sentIds.remove(id, 1);
        return contained >= 1;
    }

    public synchronized void reset()
    {
        sentIds.clear();
    }
}
