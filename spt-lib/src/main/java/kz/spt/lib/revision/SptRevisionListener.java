package kz.spt.lib.revision;

import kz.spt.lib.model.CurrentUser;
import kz.spt.lib.model.SptRevEntity;
import org.hibernate.envers.RevisionListener;
import org.springframework.security.core.context.SecurityContextHolder;

public class SptRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        SptRevEntity sptRevEntity = (SptRevEntity) revisionEntity;
        sptRevEntity.setUsername("system");
        if(SecurityContextHolder.getContext().getAuthentication()!=null && SecurityContextHolder.getContext().getAuthentication().getPrincipal()!=null){
            if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof CurrentUser) {
                CurrentUser currentUser = (CurrentUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                if(currentUser!=null){
                    sptRevEntity.setUsername(currentUser.getUsername());
                }
            }
        }
    }
}
