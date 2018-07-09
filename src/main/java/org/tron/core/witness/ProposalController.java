package org.tron.core.witness;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.tron.core.capsule.BlockCapsule;
import org.tron.core.capsule.ProposalCapsule;
import org.tron.core.db.Manager;
import org.tron.protos.Protocol.Proposal.State;

@Slf4j
public class ProposalController {

  @Setter
  @Getter
  private Manager manager;

  public static ProposalController createInstance(Manager manager) {
    ProposalController instance = new ProposalController();
    instance.setManager(manager);
    return instance;
  }


  public void processProposals(long currentMaintenanceTime) {
    long latestProposalNum = manager.getDynamicPropertiesStore().getLatestProposalNum();
    if (latestProposalNum == 0) {
      logger.info("latestProposalNum is 0,return");
      return;
    }

    long proposalNum = latestProposalNum;

    ProposalCapsule proposalCapsule;

    while (proposalNum > 0) {
      proposalCapsule = manager.getProposalStore()
          .get(ProposalCapsule.calculateDbKey(proposalNum));
      if (proposalCapsule.hasProcessed()) {
        logger
            .info("Proposal has processed，id:[{}],skip it and before it", proposalCapsule.getID());
        //proposals with number less than this one, have been processed before
        break;
      }

      if (proposalCapsule.hasCanceled()) {
        logger.info("Proposal has canceled，id:[{}],skip it", proposalCapsule.getID());
        proposalNum--;
        continue;
      }

      if (proposalCapsule.hasExpired(currentMaintenanceTime)) {
        processProposal(proposalCapsule);
        proposalNum--;
        continue;
      }

      logger.info("Proposal has not expired，id:[{}],skip it", proposalCapsule.getID());
    }
    logger.info("Process proposals done, oldest proposal[{}]", proposalNum);
  }

  public void processProposal(ProposalCapsule proposalCapsule) {

    if (proposalCapsule.hasMostApprovals()) {
      logger.info(
          "Process proposal,id:{},it has received most approvals ,begins to set dynamic parameter,{},and set  proposal state as DISAPPROVED",
          proposalCapsule.getID(), proposalCapsule.getParameters());
      setDynamicParameters(proposalCapsule);
      proposalCapsule.setState(State.APPROVED);
      manager.getProposalStore().put(proposalCapsule.createDbKey(), proposalCapsule);
    } else {
      logger.info(
          "Process proposal,id:{},it has not received enough approvals,set proposal state as DISAPPROVED",
          proposalCapsule.getID());
      proposalCapsule.setState(State.DISAPPROVED);
      manager.getProposalStore().put(proposalCapsule.createDbKey(), proposalCapsule);
    }

  }

  public void setDynamicParameters(ProposalCapsule proposalCapsule) {
    Map<Long, Long> map = proposalCapsule.getInstance().getParametersMap();
    for (Map.Entry<Long, Long> entry : map.entrySet()) {

      switch (entry.getKey().intValue()) {
        case (1): {
          manager.getDynamicPropertiesStore().saveAccountUpgradeCost(entry.getValue());
          break;
        }
        case (2): {
          manager.getDynamicPropertiesStore().saveCreateAccountFee(entry.getValue());
          break;
        }
        case (3): {
          manager.getDynamicPropertiesStore().saveTransactionFee(entry.getValue());
          break;
        }
        default:
          break;
      }
    }
  }


}
