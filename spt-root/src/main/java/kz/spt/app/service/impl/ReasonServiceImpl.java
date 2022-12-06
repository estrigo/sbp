package kz.spt.app.service.impl;

import kz.spt.app.repository.ReasonsRepository;
import kz.spt.app.service.ReasonsService;
import kz.spt.lib.model.Reasons;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(noRollbackFor = Exception.class)
public class ReasonServiceImpl implements ReasonsService {

    private ReasonsRepository reasonsRepository;

    public ReasonServiceImpl (ReasonsRepository reasonsRepository) {
        this.reasonsRepository = reasonsRepository;
    }

    public List<Reasons> findAllReasons() {
        return reasonsRepository.findAll();
    }

    public void addNewReason(String reason) {
        Reasons reasons = new Reasons();
        reasons.setReasonEn(reason);
        reasons.setReasonRu(reason);
        reasonsRepository.save(reasons);
    }

    public void deleteReasonById(Long id) {
        reasonsRepository.deleteById(id);
    }

}
