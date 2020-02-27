package org.tron.common.application;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.tron.common.logsfilter.EventPluginLoader;
import org.tron.common.parameter.CommonParameter;
import org.tron.core.ChainBaseManager;
import org.tron.core.config.args.Args;
import org.tron.core.consensus.ConsensusService;
import org.tron.core.db.BlockStore;
import org.tron.core.db.Manager;
import org.tron.core.metrics.blockchain.BlockChainMetricManager;
import org.tron.core.net.TronNetService;

@Slf4j(topic = "app")
@Component
public class ApplicationImpl implements Application {

  private BlockStore blockStoreDb;
  private ServiceContainer services;

  @Autowired
  private TronNetService tronNetService;

  @Autowired
  private Manager dbManager;

  @Autowired
  private ChainBaseManager chainBaseManager;

  @Autowired
  private ConsensusService consensusService;

  @Autowired
  private BlockChainMetricManager blockChainMetricManager;

  private boolean isProducer;

  @Override
  public void setOptions(Args args) {
    // not used
  }

  @Override
  @Autowired
  public void init(CommonParameter parameter) {
    blockStoreDb = dbManager.getBlockStore();
    services = new ServiceContainer();
  }

  @Override
  public void addService(Service service) {
    services.add(service);
  }

  @Override
  public void initServices(CommonParameter parameter) {
    services.init(parameter);
  }

  /**
   * start up the app.
   */
  public void startup() {
    tronNetService.start();
    consensusService.start();
    blockChainMetricManager.init();
  }

  @Override
  public void shutdown() {
    logger.info("******** start to shutdown ********");
    tronNetService.stop();
    consensusService.stop();
    synchronized (dbManager.getRevokingStore()) {
      closeRevokingStore();
      closeAllStore();
    }
    dbManager.stopRePushThread();
    dbManager.stopRePushTriggerThread();
    EventPluginLoader.getInstance().stopPlugin();
    logger.info("******** end to shutdown ********");
  }

  @Override
  public void startServices() {
    services.start();
  }

  @Override
  public void shutdownServices() {
    services.stop();
  }

  @Override
  public BlockStore getBlockStoreS() {
    return blockStoreDb;
  }

  @Override
  public Manager getDbManager() {
    return dbManager;
  }

  @Override
  public ChainBaseManager getChainBaseManager() {
    return chainBaseManager;
  }

  public boolean isProducer() {
    return isProducer;
  }

  public void setIsProducer(boolean producer) {
    isProducer = producer;
  }

  private void closeRevokingStore() {
    logger.info("******** start to closeRevokingStore ********");
    dbManager.getRevokingStore().shutdown();
  }

  private void closeAllStore() {
    dbManager.closeAllStore();
  }

}
